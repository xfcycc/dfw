package com.dfw.application;

import com.dfw.domain.*;
import com.dfw.interfaces.vo.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/** 只读取公开局面和本人私有投影的规则机器人，不接触内部房间、隐藏随机数或其他手牌。 */
public final class BotPlanner {
    private final PublicRoomVo state;
    private final PrivatePlayerVo own;
    private final PublicPlayerVo me;
    private final Room economics = new Room();
    private final int horizon;
    private final Map<String, double[]> visits = new HashMap<>();
    private final double reserve;
    private final Map<String, Double> reserves = new HashMap<>();
    private final Map<String, double[]> futureValues = new HashMap<>();
    private final Set<String> previousQuotes;
    private final boolean conservative;

    /** 输入安全投影、本人报价记忆与降级标记；保守模式跳过策略计算，正常模式预计算有界前瞻。 */
    public BotPlanner(PublicRoomVo state, PrivatePlayerVo own, Map<String, Integer> previousQuotes, boolean conservative) {
        this.state = state; this.own = own; me = person(own.playerId);
        this.previousQuotes = Set.copyOf(previousQuotes.keySet()); this.conservative = conservative;
        horizon = switch (me.botDifficulty) { case "easy" -> 1; case "hard" -> 6; default -> 3; };
        economics.economy = state.economy;
        if (conservative) { reserve = 0; return; }
        for (PublicPlayerVo p : state.players) if (!p.bankrupt) visits.put(p.id, forecast(p));
        for (PublicPlayerVo p : state.players) if (!p.bankrupt) reserves.put(p.id, cashReserve(p));
        reserve = reserves.get(me.id);
    }

    /** 输入近期是否已主动报价，返回下一合法意图；非本人等待点返回空。 */
    public Action next(boolean offered) {
        if (state.recoveryPending || state.pausedReason != null || !state.status.equals("playing") || me.bankrupt) return null;
        if (state.tradePending) {
            if (own.trade == null || !own.trade.toId.equals(me.id)) return null;
            Action a = action("tradeRespond"); a.accept = !conservative && acceptTrade(own.trade); return a;
        }
        if (own.choice != null) return choice(conservative);
        if (state.activity != null && !state.activity.stage.equals("payment")) return activity(conservative);
        if (state.pending != null && !state.pending.playerId.equals(me.id)) return null;
        if (state.pending == null && !state.turnOrder.get(state.turnIndex).equals(me.id)) return null;
        if (state.pending != null && Set.of("rent", "debt").contains(state.pending.type)) return payment(offered, conservative);
        if (Set.of("buy", "upgrade").contains(state.phase)) {
            if (conservative) return action("pass");
            Tile tile = state.board.get(state.pending.tileId);
            long cost = state.phase.equals("buy") ? tile.price : Rules.buildCost(economics, tile);
            double gain = state.phase.equals("buy") ? value(tile, me.id) : upgradeValue(tile, tile.level + 1);
            return action(!conservative && me.cash >= cost && me.cash - cost >= reserve && gain >= cost * (horizon == 1 ? .8 : .95) ? state.phase : "pass");
        }
        if (!conservative) {
            if (state.phase.equals("pre_roll") && !own.activeCardUsed) { Action card = activeCard(); if (card != null) return card; }
            if (!offered) { Action trade = offer(); if (trade != null) return trade; }
            Tile redeem = state.board.stream().filter(t -> me.id.equals(t.ownerId) && !PropertyEffects.frozen(t) && t.mortgaged && me.cash - Rules.redeemCost(t) >= reserve)
                    .max(Comparator.comparingDouble(t -> rentWhenActive(t, t.level) * traffic(t, me.id) / Math.max(1, Rules.redeemCost(t)))).orElse(null);
            if (redeem != null) return tileAction("redeem", redeem.id);
        }
        return action(state.phase.equals("pre_roll") ? "roll" : "endTurn");
    }

    /** 输入保守标记，完成本人七类内容选择；未知类型作为规划错误报告。 */
    private Action choice(boolean conservative) {
        ContentChoiceVo c = own.choice; Action a = action("contentDecision"); a.decisionId = c.id; a.accept = true;
        switch (c.type) {
            case "hand_limit" -> a.keepCardIds = (conservative ? c.cards.stream() : c.cards.stream().sorted(Comparator.comparingDouble(this::keepValue).reversed())).limit(c.handLimit > 0 ? c.handLimit : 6).map(card -> card.id).toList();
            case "free_upgrade", "full_upgrade" -> {
                a.tileId = conservative ? null : c.tileIds.stream().max(Comparator.comparingDouble(id -> upgradeValue(state.board.get(id), c.type.equals("full_upgrade") ? Rules.MAX_LEVEL : state.board.get(id).level + 1))).orElse(null);
                a.accept = a.tileId != null;
            }
            case "self_destroy", "opponent_destroy" -> a.tileId = conservative ? c.tileIds.stream().findFirst().orElse(null) : c.tileIds.stream().max(Comparator.comparingDouble(id -> {
                Tile t = state.board.get(id); double loss = value(t, t.ownerId) - valueAtLevel(t, t.ownerId, 0);
                return c.type.equals("self_destroy") ? -loss : loss * threat(person(t.ownerId));
            })).orElse(null);
            case "discover" -> a.cardId = (conservative ? c.cards.stream().findFirst() : c.cards.stream().max(Comparator.comparingDouble(this::keepValue))).orElseThrow().id;
            case "recycle" -> {
                if(conservative) a.accept=false;
                else { a.discardCardIds=c.cards.stream().filter(card -> keepValue(card)<=startIncome()*.3).map(card -> card.id).toList(); if(a.discardCardIds.isEmpty()) a.discardCardIds=List.of(c.cards.stream().min(Comparator.comparingDouble(this::keepValue)).orElseThrow().id); }
            }
            case "gamble" -> a.accept = !conservative && gamble(c.content);
            case "release" -> { a.cardId = own.hand.stream().filter(card -> card.kind.equals("release")).map(card -> card.id).findFirst().orElse(null); a.accept = a.cardId != null; }
            default -> throw new GameError("机器人尚未支持的私人选择：" + c.type);
        }
        return a;
    }

    /** 输入报价频率和保守标记，先防御再付款；抵押筹款不足时选择卖房或整售，避免误申请清算。 */
    private Action payment(boolean offered, boolean conservative) {
        PendingDecision debt = state.pending;
        if (!debt.defenseResolved) {
            Action a = action("paymentDefense"); a.decisionId = debt.id; double best = 0;
            for (Card c : own.hand) {
                double saving = c.kind.equals("immunity") && debt.type.equals("rent") ? debt.amount : c.kind.equals("halfPayment") ? debt.amount / 2 : 0;
                double score = saving - (conservative || me.cash < debt.amount ? 0 : keepValue(c) * .5);
                if (score > best) { best = score; a.cardId = c.id; }
            }
            return a;
        }
        if (me.cash >= debt.amount) return action("payDebt");
        if (!offered && !conservative && debt.creditorId != null) { Action trade = offer(); if (trade != null) return trade; }
        /*
         * 先抵押再出售会多付赎回费用；全部抵押仍不够时，直接选择更高收益的出售路径，
         * 避免本来整售能偿清，却因先抵押降低最终可用现金。
         */
        long mortgageCash = me.cash + state.board.stream().filter(t -> me.id.equals(t.ownerId) && !PropertyEffects.frozen(t) && !t.mortgaged).mapToLong(t -> Rules.mortgageValue(economics, t)).sum();
        if (mortgageCash < debt.amount) {
            var sales = state.board.stream().filter(t -> me.id.equals(t.ownerId) && !PropertyEffects.frozen(t) && Rules.propertyLiquidationValue(economics, t) > (t.mortgaged ? 0 : Rules.mortgageValue(economics, t)));
            Tile sale = (conservative ? sales.findFirst() : sales.min(Comparator.comparingDouble(t -> rentWhenActive(t, t.level) * traffic(t, me.id) / Math.max(1, Rules.propertyLiquidationValue(economics, t))))).orElse(null);
            if (sale != null) {
                boolean sellBuilding = !sale.mortgaged && sale.level > 0 && Rules.propertyLiquidationValue(economics, sale) > Math.max(Rules.mortgageValue(economics, sale), Rules.landValue(economics, sale));
                return tileAction(sellBuilding ? "sellBuilding" : "sellLand", sale.id);
            }
        }
        var available = state.board.stream().filter(t -> me.id.equals(t.ownerId) && !PropertyEffects.frozen(t) && !t.mortgaged);
        Tile mortgage = (conservative ? available.findFirst() : available.min(Comparator.comparingDouble(t -> rent(t, t.level) * traffic(t, me.id) / Math.max(1, Rules.mortgageValue(economics, t))))).orElse(null);
        if (mortgage != null) return tileAction("mortgage", mortgage.id);
        return action("declareBankruptcy");
    }

    /** 输入本人收到的报价，使用与主动报价相同的现金、排名及交易后连片评分决定是否接受。 */
    private boolean acceptTrade(Trade t) { return tradeScore(t, me) >= tradeMargin(t, me); }

    /** 无入参，枚举买卖、互换和抵债，按同一评估挑选本人有收益且对方可能接受的一份报价。 */
    private Action offer() {
        Action best = null; double bestGain = startIncome() * .025;
        boolean debt = state.pending != null && Set.of("rent", "debt").contains(state.pending.type);
        for (PublicPlayerVo other : state.players) if (!other.bankrupt && other.online && !other.id.equals(me.id)) {
            for (Tile t : state.board) if (t.type.equals("property") && !PropertyEffects.frozen(t) && (me.id.equals(t.ownerId) || !debt && other.id.equals(t.ownerId))) {
                Action assets = trade(other.id);
                if (me.id.equals(t.ownerId)) assets.giveTiles = List.of(t.id); else assets.wantTiles = List.of(t.id);
                Action candidate = negotiate(assets, other, false);
                if (candidate != null) {
                    double gain = tradeScore(quote(candidate), me);
                    if (gain > bestGain) { best = candidate; bestGain = gain; }
                }
                if (debt && other.id.equals(state.pending.creditorId)) {
                    candidate = negotiate(assets, other, true);
                    if (candidate != null) {
                        double gain = tradeScore(quote(candidate), me);
                        if (gain > bestGain) { best = candidate; bestGain = gain; }
                    }
                }
            }
            if (!debt) for (Tile give : state.board) if (me.id.equals(give.ownerId) && !PropertyEffects.frozen(give)) for (Tile want : state.board) if (other.id.equals(want.ownerId) && !PropertyEffects.frozen(want)) {
                Action assets = trade(other.id); assets.giveTiles = List.of(give.id); assets.wantTiles = List.of(want.id);
                Action candidate = negotiate(assets, other, false);
                if (candidate != null) {
                    double gain = tradeScore(quote(candidate), me);
                    if (gain > bestGain) { best = candidate; bestGain = gain; }
                }
            }
        }
        return best;
    }

    /** 输入交换资产、对方及是否抵债，围绕双方基础估值和综合评分的中点试算有限价格，返回最佳可接受报价。 */
    private Action negotiate(Action assets, PublicPlayerVo other, boolean credit) {
        Trade base = quote(assets);
        double mine = tradeScore(base, me), theirs = tradeScore(base, other);
        if (!Double.isFinite(mine) || !Double.isFinite(theirs)) return null;
        boolean pay = !credit && mine > theirs;
        PublicPlayerVo buyer = pay ? me : other;
        PublicPlayerVo seller = pay ? other : me;
        long limit = credit ? state.pending.amount : Math.max(0, buyer.cash - (long)Math.ceil(reserves.get(buyer.id) * tradeWeight(buyer, .2, .45, .65)));
        double plainMine = tileValues(assets.wantTiles, me.id) - tileValues(assets.giveTiles, me.id);
        double plainTheirs = tileValues(assets.giveTiles, other.id) - tileValues(assets.wantTiles, other.id);
        double discount = 1 - cashPressure(seller) * tradeWeight(me, .05, .12, .2) - cashPressure(buyer) * tradeWeight(me, .03, .08, .12);
        Set<Long> prices = new LinkedHashSet<>(); prices.add(0L); prices.add(limit);
        for (double center : new double[] { Math.abs(mine - theirs) / 2, Math.abs(plainMine - plainTheirs) / 2 * discount })
            for (double factor : new double[] { .8, 1, 1.2 }) prices.add(Math.max(0, Math.min(limit, Math.round(center * factor))));
        Action best = null; double bestGain = startIncome() * .025;
        for (long price : prices) {
            Action candidate = trade(other.id); candidate.giveTiles = assets.giveTiles; candidate.wantTiles = assets.wantTiles;
            if (credit) candidate.debtCredit = price;
            else if (pay) candidate.giveCash = price;
            else candidate.wantCash = price;
            if (repeated(candidate)) continue;
            Trade t = quote(candidate); double gain = tradeScore(t, me);
            if (gain >= tradeMargin(t, me) && gain > bestGain && tradeScore(t, other) >= tradeMargin(t, other)) { best = candidate; bestGain = gain; }
        }
        return best;
    }

    /** 输入报价与评估者，综合本人收益、对方收益及双方排名变化；现金不足时返回不可接受分数。 */
    private double tradeScore(Trade t, PublicPlayerVo actor) {
        PublicPlayerVo from = person(t.fromId), to = person(t.toId), other = actor.id.equals(t.fromId) ? to : from;
        if (from.cash < t.giveCash || to.cash < t.wantCash) return Double.NEGATIVE_INFINITY;
        long delta = cashChange(t, actor);
        if (delta < 0 && actor.cash + delta < reserves.get(actor.id) * tradeWeight(actor, .2, .45, .65)) return Double.NEGATIVE_INFINITY;
        double mine = tradeBenefit(t, actor), theirs = tradeBenefit(t, other);
        double ownAfter = actor.netWorth + worthChange(t, actor), otherAfter = other.netWorth + worthChange(t, other);
        int ownRank = rank(actor, actor.netWorth, other, other.netWorth), otherRank = rank(other, other.netWorth, actor, actor.netWorth);
        int ownRankAfter = rank(actor, ownAfter, other, otherAfter), otherRankAfter = rank(other, otherAfter, actor, ownAfter);
        double threat = 1 + (otherRank < ownRank ? .5 : 0) + .25 * Math.min(2, other.netWorth / (double)Math.max(1, actor.netWorth));
        double gapChange = Math.max(0, otherAfter - ownAfter) - Math.max(0, other.netWorth - actor.netWorth);
        double rankCost = gapChange * tradeWeight(actor, .01, .03, .06)
                + (ownRankAfter - ownRank + (otherRank - otherRankAfter) * .5) * startIncome() * tradeWeight(actor, .02, .06, .12);
        return mine - Math.max(0, theirs) * threat * tradeWeight(actor, .02, .08, .18) - rankCost;
    }

    /** 输入报价与一方，返回现金、地产、抵债、资金压力及连片变化带来的主观收益，不改变实际资产或租金。 */
    private double tradeBenefit(Trade t, PublicPlayerVo actor) {
        boolean from = actor.id.equals(t.fromId);
        double property = tileValues(from ? t.wantTiles : t.giveTiles, actor.id) - tileValues(from ? t.giveTiles : t.wantTiles, actor.id);
        double debt = from ? t.debtCredit : -t.debtCredit;
        double beforeNeed = paymentNeed(actor), afterNeed = beforeNeed;
        if (state.pending != null && actor.id.equals(state.pending.playerId) && Objects.equals(t.debtId, state.pending.id)) afterNeed -= t.debtCredit;
        double liquidity = (Math.max(0, beforeNeed - actor.cash) - Math.max(0, afterNeed - actor.cash - cashChange(t, actor))) * tradeWeight(actor, .3, .7, 1.1);
        return cashChange(t, actor) + property + debt + liquidity + clusterValue(actor, t) - clusterValue(actor, null);
    }

    /** 输入玩家与可选报价，按交易前后真实相邻地产计算策略价值；特殊格中断，抵押地产折减，不增加实际租金。 */
    private double clusterValue(PublicPlayerVo actor, Trade t) {
        double result = 0; int size = state.board.size();
        for (int i = 0; i < size; i++) {
            Tile current = state.board.get(i), next = state.board.get((i + 1) % size), previous = state.board.get((i + size - 1) % size);
            if (!ownedAfter(current, actor.id, t) || !ownedAfter(next, actor.id, t)) continue;
            double active = current.mortgaged || next.mortgaged ? .35 : 1;
            result += Math.min(current.price, next.price) * active;
            if (ownedAfter(previous, actor.id, t)) result += Math.min(previous.price, Math.min(current.price, next.price)) * active * (previous.mortgaged ? .175 : .5);
        }
        return result * tradeWeight(actor, .04, .12, .24);
    }

    /** 输入地产、玩家与可选报价，判断交易后归属；仅查看本次报价列明的地产与公开产权。 */
    private boolean ownedAfter(Tile tile, String id, Trade t) {
        if (!tile.type.equals("property")) return false;
        String owner = t == null ? tile.ownerId : t.giveTiles.contains(tile.id) ? t.toId : t.wantTiles.contains(tile.id) ? t.fromId : tile.ownerId;
        return id.equals(owner);
    }

    /** 输入报价和玩家，返回真实公开净资产的变化；待付债务不在现有排行榜口径中，不把抵债虚算成现金。 */
    private double worthChange(Trade t, PublicPlayerVo actor) {
        boolean from = actor.id.equals(t.fromId); double result = cashChange(t, actor);
        for (int id : from ? t.wantTiles : t.giveTiles) result += equity(state.board.get(id));
        for (int id : from ? t.giveTiles : t.wantTiles) result -= equity(state.board.get(id));
        return result;
    }

    /** 输入地产，返回与正式排行榜一致的成本权益，抵押时扣固定赎回费用。 */
    private long equity(Tile tile) { return Math.max(0, Rules.propertyValue(economics, tile) - (tile.mortgaged ? Rules.redeemCost(tile) : 0)); }
    /** 输入双方假设净资产，返回该玩家在所有未破产玩家中的名次，并列沿用相同排名。 */
    private int rank(PublicPlayerVo actor, double worth, PublicPlayerVo other, double otherWorth) {
        return 1 + (int)state.players.stream().filter(p -> !p.bankrupt && !p.id.equals(actor.id) && (p.id.equals(other.id) ? otherWorth : p.netWorth) > worth).count();
    }
    /** 输入报价和一方，返回本次真实现金变化，不包含约定抵债。 */
    private long cashChange(Trade t, PublicPlayerVo actor) { return actor.id.equals(t.fromId) ? t.wantCash - t.giveCash : t.giveCash - t.wantCash; }
    /** 输入报价与评估者，返回对应难度要求的最低综合收益。 */
    private double tradeMargin(Trade t, PublicPlayerVo actor) {
        boolean from = actor.id.equals(t.fromId);
        double paid = (from ? t.giveCash : t.wantCash + t.debtCredit) + tileValues(from ? t.giveTiles : t.wantTiles, actor.id);
        return Math.max(1, paid) * tradeWeight(actor, -.1, .02, .04);
    }
    /** 输入公开玩家与三档权重，返回该难度的权重；真人的交易意愿以普通策略估计，不读取私人意图。 */
    private double tradeWeight(PublicPlayerVo actor, double easy, double normal, double hard) { return "easy".equals(actor.botDifficulty) ? easy : "hard".equals(actor.botDifficulty) ? hard : normal; }
    /** 输入公开玩家，返回风险储备加本人当前待付费用，不将他人的债务算给该玩家。 */
    private double paymentNeed(PublicPlayerVo actor) {
        return reserves.get(actor.id) + (state.pending != null && Set.of("rent", "debt").contains(state.pending.type) && actor.id.equals(state.pending.playerId) ? state.pending.amount : 0);
    }
    /** 输入公开玩家，返回零至一的现金紧张程度，用于生成受资金压力影响的议价候选。 */
    private double cashPressure(PublicPlayerVo actor) { return Math.max(0, Math.min(1, 1 - actor.cash / Math.max(1, paymentNeed(actor)))); }
    /** 输入公开玩家，返回按未来落点、租金和难度计算的现金储备，不读取手牌。 */
    private double cashReserve(PublicPlayerVo actor) {
        double exposure = 0, immediate = 0;
        for (Tile tile : state.board) if (tile.ownerId != null && !tile.ownerId.equals(actor.id)) {
            exposure += visits.get(actor.id)[tile.id] * rent(tile, tile.level);
            for (int point = 1; point <= 6; point++) if (destination(actor, actor.position, point, null, true) == tile.id) immediate = Math.max(immediate, rent(tile, tile.level));
        }
        return startIncome() * tradeWeight(actor, .25, .55, .55) + exposure * tradeWeight(actor, .4, .4, .8) + immediate * tradeWeight(actor, .15, .65, .65);
    }
    /** 输入本人拟发出的操作，转换为只用于评估的报价，沿用当前费用身份，不消费正式随机数。 */
    private Trade quote(Action a) {
        Trade t = new Trade(); t.fromId = me.id; t.toId = a.toId; t.giveCash = a.giveCash; t.wantCash = a.wantCash;
        t.giveTiles = a.giveTiles; t.wantTiles = a.wantTiles; t.debtCredit = a.debtCredit;
        if (state.pending != null && Set.of("rent", "debt").contains(state.pending.type) && me.id.equals(state.pending.playerId)) t.debtId = state.pending.id;
        return t;
    }

    /** 无入参，按使用时机和未来机会成本比较全部支持的主动卡及合法目标，返回最高收益行动。 */
    private Action activeCard() {
        Action best = null; double bestScore = 0;
        for (Card c : own.hand) {
            Action candidate = action("card"); candidate.cardId = c.id; double score = Double.NEGATIVE_INFINITY;
            switch (c.kind) {
                case "remote" -> {
                    for (int point = 1; point <= 6; point++) {
                        double gain = movementValue(me, me.position, point) - rollValue(me, me.position, 0);
                        if (gain > score) { score = gain; candidate.point = point; }
                    }
                }
                case "fixed", "move", "stay" -> score = movementValue(me, me.position, parameter(c, "steps", 0).intValue()) - rollValue(me, me.position, 0);
                case "bonus" -> score = rollValue(me, me.position, parameter(c, "steps", 0).intValue()) - rollValue(me, me.position, 0);
                case "cash" -> score = parameter(c, "amount", 0);
                case "gamble" -> { if (gamble(c)) score = parameter(c, "prize", 0) * parameter(c, "probability", 0) / 100.0 - parameter(c, "wager", 0); }
                case "freeUpgrade", "fullUpgrade" -> {
                    for (Tile t : state.board) if (me.id.equals(t.ownerId) && !PropertyEffects.frozen(t) && !t.mortgaged && t.level < Rules.MAX_LEVEL && !state.upgradedTiles.contains(t.id)) {
                        double gain = upgradeValue(t, c.kind.equals("fullUpgrade") ? Rules.MAX_LEVEL : t.level + 1); if (gain > score) { score = gain; candidate.tileId = t.id; }
                    }
                }
                case "demolish", "fullDemolish" -> {
                    for (Tile t : state.board) if (t.ownerId != null && !PropertyEffects.frozen(t) && !t.demolitionShield && !me.id.equals(t.ownerId) && !person(t.ownerId).bankrupt && t.level > 0) {
                        double gain = (value(t, t.ownerId) - valueAtLevel(t, t.ownerId, c.kind.equals("fullDemolish") ? 0 : t.level - 1)) * threat(person(t.ownerId));
                        if (gain > score) { score = gain; candidate.tileId = t.id; }
                    }
                }
                case "barrier" -> {
                    for (Tile t : state.board) if (!PropertyEffects.frozen(t) && !t.barrier && t.id != me.position && GameSupport.nearbyDistance(me.position, t.id, state.board.size()) <= 6) {
                        double gain = 0;
                        for (PublicPlayerVo p : state.players) if (!p.bankrupt && p.skipTurns == 0) {
                            double delta = 0;
                            for (int point = 1; point <= 6; point++) delta += (landingValue(p, destination(p, p.position, point, t.id, true)) - landingValue(p, destination(p, p.position, point, null, true))) / 6;
                            gain += me.id.equals(p.id) ? delta : -delta * threat(p);
                        }
                        if (gain > score) { score = gain; candidate.tileId = t.id; }
                    }
                }
                case "swap", "blast", "jail", "steal" -> {
                    for (PublicPlayerVo p : state.players) if (!p.bankrupt) {
                        boolean self = p.id.equals(me.id); double gain;
                        if (c.kind.equals("swap")) {
                            if (self || p.jailed) continue;
                            gain = rollValue(me, p.position, 0) - rollValue(me, me.position, 0) - threat(p) * (rollValue(p, me.position, 0) - rollValue(p, p.position, 0));
                        } else if (c.kind.equals("blast")) {
                            double average = 0; for (int i = 0; i < state.board.size(); i++) average += rollValue(p, i, 0) / state.board.size();
                            gain = (average - rollValue(p, p.position, 0)) * (self ? 1 : -threat(p));
                        } else if (c.kind.equals("jail")) {
                            double income = state.board.stream().filter(t -> p.id.equals(t.ownerId)).mapToDouble(t -> rent(t, t.level) * traffic(t, p.id) / horizon).sum();
                            gain = self ? -rollValue(me, me.position, 0) - income - startIncome() * .4 : threat(p) * (income + startIncome() * .4) / (p.jailed ? 4 : 1);
                        } else {
                            if (self || p.cardCount == null || p.cardCount == 0) continue;
                            gain = startIncome() * .25 * (1 + threat(p));
                        }
                        if (gain > score) { score = gain; candidate.playerId = p.id; }
                    }
                }
                default -> score = expansionCard(c,candidate);
            }
            double threshold = c.kind.equals("cash") ? 0 : keepValue(c) * (own.hand.size() >= (me.handLimit == null ? 6 : me.handLimit) ? .1 : .35);
            double adjusted = score - threshold;
            if (horizon == 1 && adjusted > 0) adjusted *= ThreadLocalRandom.current().nextDouble(.65, 1.35);
            if (adjusted > bestScore) { bestScore = adjusted; best = candidate; }
        }
        return best;
    }

    /** 输入新增主动卡和待填命令，仅按公开地产、位置、手牌数量和本人牌面评分并设置合法目标。 */
    private double expansionCard(Card card, Action action) {
        double best=Double.NEGATIVE_INFINITY, base=startIncome()*.25;
        switch(card.kind) {
            case "discover" -> { return base*1.5; }
            case "recycle" -> { return own.hand.stream().anyMatch(c -> !c.id.equals(card.id) && keepValue(c)<=base) ? base : best; }
            case "handDestruction" -> { return base * (state.players.stream().filter(p -> !p.bankrupt && !p.id.equals(me.id)).mapToInt(p -> p.cardCount==null?0:p.cardCount).average().orElse(0) - Math.max(0,own.hand.size()-1) + .5); }
            case "meteor" -> { return state.board.stream().anyMatch(t -> t.type.equals("property") && !PropertyEffects.frozen(t)) ? base : best; }
            case "gravity" -> {
                double gain=0; for(PublicPlayerVo p:state.players) if(!p.bankrupt && !p.id.equals(me.id)) gain-=threat(p)*(rollValue(p,me.position,0)-rollValue(p,p.position,0));
                return gain;
            }
            case "swapAny" -> {
                for(PublicPlayerVo p:state.players) if(!p.bankrupt) for(PublicPlayerVo q:state.players) if(!q.bankrupt && !p.id.equals(q.id) && p.position!=q.position) {
                    double gain=(p.id.equals(me.id)?1:-threat(p))*(rollValue(p,q.position,0)-rollValue(p,p.position,0))
                            +(q.id.equals(me.id)?1:-threat(q))*(rollValue(q,p.position,0)-rollValue(q,q.position,0));
                    if(gain>best) { best=gain; action.playerId=p.id; action.secondPlayerId=q.id; }
                }
            }
            case "sap","slow","reaper","shadowstep" -> {
                for(PublicPlayerVo p:state.players) if(!p.bankrupt && !p.id.equals(me.id)) {
                    double gain;
                    switch(card.kind) {
                        case "sap" -> { if(p.position!=me.position) continue; gain=base*(1+threat(p)+(p.cardCount!=null && p.cardCount>0?1:0)); }
                        case "slow" -> {
                            if(p.nextRollHalf) continue;
                            gain=0; for(int point=1;point<=6;point++) gain+=threat(p)*(movementValue(p,p.position,point)-movementValue(p,p.position,Math.floorDiv(point,2)))/6;
                        }
                        case "reaper" -> { if((me.handLimit==null?6:me.handLimit)<=1 || (p.handLimit==null?6:p.handLimit)<=1) continue; gain=base*(threat(p)+.5); }
                        case "shadowstep" -> gain=landingValue(me,p.position)+(p.position==0?startIncome():0)-rollValue(me,me.position,0);
                        default -> throw new IllegalStateException("未处理的新增玩家道具");
                    }
                    if(gain>best) { best=gain; action.playerId=p.id; }
                }
            }
            case "shield","exchange","kamui","blackhole","homeTeleport" -> {
                for(Tile t:state.board) if(t.type.equals("property") && !PropertyEffects.frozen(t)) {
                    boolean self=me.id.equals(t.ownerId); double gain;
                    switch(card.kind) {
                        case "shield" -> { if(!self || t.demolitionShield || t.level==0) continue; gain=Rules.buildCost(economics,t)*t.level*.5; }
                        case "exchange" -> { long cost=PropertyEffects.baseRent(economics,t); if(t.ownerId==null || self || me.cash<cost) continue; gain=value(t,me.id)-cost; }
                        case "kamui" -> { if(t.ownerId==null || self) continue; gain=rent(t,t.level)*traffic(t,t.ownerId); }
                        case "blackhole" -> {
                            gain=(self?-1:1)*(t.demolitionShield?base:0)+(self?-1:1)*(t.concertStacks>0?base*t.concertStacks:0);
                            if(t.barrier || t.meteorCharges>0) gain+=base*(self?-1:1);
                            if(t.treasure) gain-=base;
                            if(!t.demolitionShield && t.concertStacks==0 && !t.barrier && t.meteorCharges==0 && !t.treasure) continue;
                        }
                        case "homeTeleport" -> {
                            if(!self) continue; gain=-rollValue(me,me.position,0);
                            if(!t.mortgaged && t.level<Rules.MAX_LEVEL && !state.upgradedTiles.contains(t.id) && me.cash>=Rules.buildCost(economics,t)) gain+=Math.max(0,upgradeValue(t,t.level+1)-Rules.buildCost(economics,t));
                            if(t.treasure) gain+=PropertyEffects.baseRent(economics,t);
                        }
                        default -> throw new IllegalStateException("未处理的新增地产道具");
                    }
                    if(gain>best) { best=gain; action.tileId=t.id; }
                }
            }
            default -> { }
        }
        return best;
    }

    /** 输入保守标记，仅用已公开活动阶段与本人现金选择，秘密猜测使用独立均匀随机数。 */
    private Action activity(boolean conservative) {
        ActivityVo a = state.activity;
        if (a.endsAt != null || !a.participants.contains(me.id)) return null;
        if (a.kind.equals("roulette")) {
            if (!me.id.equals(a.stage.equals("select") ? a.triggerId : a.currentPlayerId) || !Set.of("select", "duel").contains(a.stage)) return null;
        } else if (a.kind.equals("treasure")) {
            if (!me.id.equals(a.triggerId) || !Set.of("select", "treasure_choice").contains(a.stage)) return null;
        } else {
            if ((!a.stage.equals("select") && !(a.kind.equals("bridge") && a.stage.equals("bridge_select"))) || a.submitted.contains(me.id)) return null;
            if ((a.kind.equals("rps") || a.stage.equals("bridge_select")) && !a.contenders.contains(me.id)) return null;
        }
        Action input = action("activityInput"); input.activityId = a.id; input.activityRound = a.round;
        switch (a.kind) {
            case "packet", "blindbox" -> {
                int count = a.kind.equals("packet") ? a.participants.size() : 20;
                List<Integer> available = new ArrayList<>(); for (int i = 1; i <= count; i++) if (!a.packetSelections.containsValue(i)) available.add(i);
                input.selection = available.get(ThreadLocalRandom.current().nextInt(available.size()));
            }
            case "rps" -> input.selection = ThreadLocalRandom.current().nextInt(3);
            case "minority" -> input.selection = ThreadLocalRandom.current().nextInt(2);
            case "horse" -> input.selection = !conservative && bet(money("horseStake"), money("horsePrize") / 8.0) ? ThreadLocalRandom.current().nextInt(1, 9) : 0;
            case "bridge" -> input.selection = a.stage.equals("bridge_select") ? ThreadLocalRandom.current().nextInt(1, 3) : !conservative && bet(money("horseStake"), money("horsePrize") / 8.0) ? 1 : 0;
            case "casino" -> {
                boolean small = horizon > 1 || ThreadLocalRandom.current().nextBoolean();
                input.selection = !conservative && bet(money(small ? "casinoSmallStake" : "casinoPointStake"), money(small ? "casinoSmallPrize" : "casinoPointPrize") / (small ? 2.0 : 6.0)) ? small ? 1 : 2 : 0;
                input.point = ThreadLocalRandom.current().nextInt(1, small ? 3 : 7);
            }
            case "treasure" -> {
                boolean play = !conservative && (a.level == 0 ? bet(money("treasureStake"), money("treasureStake")) : horizon == 1 && ThreadLocalRandom.current().nextBoolean());
                input.selection = !conservative && play ? ThreadLocalRandom.current().nextInt(1, 3) : 0;
            }
            case "roulette" -> {
                input.selection = 1;
                if (a.stage.equals("select")) {
                    PublicPlayerVo target = state.players.stream().filter(p -> !p.bankrupt && !p.id.equals(me.id)).max(Comparator.comparingDouble(this::threat)).orElse(null);
                    if (conservative || target == null || me.cash < reserve + money("rouletteDebt")) input.selection = 0;
                    else input.playerId = target.id;
                }
            }
            default -> throw new GameError("机器人尚未支持的角落活动：" + a.kind);
        }
        return input;
    }

    /** 输入公开玩家，返回有界前瞻中的折扣落点次数；仅枚举公平骰子，不调用正式随机源。 */
    private double[] forecast(PublicPlayerVo p) {
        int size = state.board.size(); double[] total = new double[size], distribution = new double[size]; distribution[p.position] = 1;
        for (int turn = 0; turn < horizon; turn++) {
            if (turn < p.skipTurns) continue;
            double[] next = new double[size];
            for (int from = 0; from < size; from++) if (distribution[from] > 0) for (int point = 1; point <= 6; point++) next[destination(p, from, point, null, turn == p.skipTurns)] += distribution[from] / 6;
            for (int i = 0; i < size; i++) total[i] += next[i] * Math.pow(.85, turn);
            distribution = next;
        }
        return total;
    }

    /** 输入移动者、起点、基础步数和可选路障，返回考虑移动翻倍及现有路障的预测落点。 */
    private int destination(PublicPlayerVo p, int from, int steps, Integer barrier, boolean first) {
        int base = first && p.nextRollHalf ? Math.floorDiv(steps,2) : steps;
        int finalSteps = first && p.nextMoveDouble ? base * 2 : base, position = from;
        for (int i = 0; i < Math.abs(finalSteps); i++) {
            position = Math.floorMod(position + Integer.signum(finalSteps), state.board.size());
            if (p.trapImmunityTurns == 0 && !PropertyEffects.frozen(state.board.get(position)) && (state.board.get(position).barrier || state.board.get(position).meteorCharges>0 || Objects.equals(barrier, position))) break;
        }
        return position;
    }

    /** 输入移动者和基础步数，返回落点价值与沿途起点收入；只估计尚未发生的随机事件。 */
    private double movementValue(PublicPlayerVo p, int from, int steps) {
        int to = destination(p, from, steps, null, true);
        return landingValue(p, to) + movementIncome(p, from, steps, true) + (horizon == 1 ? 0 : .85 * future(p)[to]);
    }

    /** 输入玩家、起点与步数，逐格估计起点奖励，遵守路障和向后离开起点不领奖的规则。 */
    private double movementIncome(PublicPlayerVo p, int from, int steps, boolean first) {
        int base = first && p.nextRollHalf ? Math.floorDiv(steps,2) : steps;
        int count = first && p.nextMoveDouble ? base * 2 : base, position = from; double income = 0;
        if (count == 0) return from == 0 ? startIncome() : 0;
        for (int i = 0; i < Math.abs(count); i++) {
            position = Math.floorMod(position + Integer.signum(count), state.board.size());
            if (position == 0) income += startIncome();
            if (p.trapImmunityTurns == 0 && !PropertyEffects.frozen(state.board.get(position)) && (state.board.get(position).barrier || state.board.get(position).meteorCharges>0)) break;
        }
        return income;
    }
    /** 输入公开玩家，动态规划其后续公平移动的折扣收益，比较当前行动对后续位置的影响。 */
    private double[] future(PublicPlayerVo p) {
        return futureValues.computeIfAbsent(p.id, id -> {
            int size = state.board.size(); double[] values = new double[size];
            for (int turn = 1; turn < horizon; turn++) {
                double[] next = new double[size];
                for (int from = 0; from < size; from++) for (int point = 1; point <= 6; point++) {
                    int to = destination(p, from, point, null, false);
                    next[from] += (landingValue(p, to) + movementIncome(p, from, point, false) + .85 * values[to]) / 6;
                }
                values = next;
            }
            return values;
        });
    }
    /** 输入玩家、起点及掷骰加码，返回六种公平骰子落点的平均价值。 */
    private double rollValue(PublicPlayerVo p, int from, int bonus) {
        double result = 0; for (int point = 1; point <= 6; point++) result += movementValue(p, from, point + bonus) / 6; return result;
    }

    /** 输入玩家和落点，评估可见租金、投资机会及入狱的收入损失，不推测隐藏卡面。 */
    private double landingValue(PublicPlayerVo p, int id) {
        Tile t = state.board.get(id);
        if (t.type.equals("jail")) return -startIncome() * .5;
        if (!t.type.equals("property")) return 0;
        if (PropertyEffects.frozen(t)) return 0;
        if (t.treasure) return PropertyEffects.baseRent(economics,t);
        if (t.ownerId == null) return p.cash - t.price >= reserve ? Math.max(0, value(t, p.id) - t.price) : 0;
        if (t.ownerId.equals(p.id) || person(t.ownerId).jailed) return 0;
        double amount = rent(t, t.level) * (p.nextRentDouble ? 2 : 1);
        if (p.nextPaymentWaiver) amount = 0;
        return -amount - Math.max(0, amount - p.cash) * (horizon == 6 ? 2 : 1);
    }

    /** 输入地产与持有人，返回结合公开位置、未来收入和赎回负担的持有价值。 */
    private double value(Tile t, String owner) { return valueAtLevel(t, owner, t.level); }
    /** 输入地产、假设持有人和等级，返回无状态修改的资产估值。 */
    private double valueAtLevel(Tile t, String owner, int level) {
        double capital = t.price + level * Rules.buildCost(economics, t);
        double income = rentWhenActive(t, level) * traffic(t, owner);
        double result = capital * (horizon == 1 ? .95 : 1) + income * (horizon == 1 ? .8 : 1.4);
        return t.mortgaged ? Math.max(0, result - Rules.redeemCost(t)) : result;
    }
    /** 输入地产与升级后的等级，返回新增资产和预计租金的收益。 */
    private double upgradeValue(Tile t, int level) { return valueAtLevel(t, me.id, level) - value(t, me.id); }
    /** 输入地产与持有人，累计对手前瞻落点与长期平均客流，服刑期间折减近期收入。 */
    private double traffic(Tile t, String owner) {
        double sum = 0;
        for (PublicPlayerVo p : state.players) if (!p.bankrupt && !p.id.equals(owner)) sum += visits.get(p.id)[t.id] + (horizon == 1 ? 1 : 3) / (double) state.board.size();
        PublicPlayerVo holder = person(owner); return holder.jailed ? sum * .6 : sum;
    }
    /** 输入地产与假设等级，按本局公开经济规则计算未抵押租金。 */
    private long rentWhenActive(Tile t, int level) { return state.economy.derived("rent." + t.price + "." + level, t.price * state.economy.rentBasePercent / 100 * Rules.RENT_MULTIPLIERS[level]); }
    /** 输入地产与等级，返回实际抵押状态下的租金。 */
    private long rent(Tile t, int level) { return t.mortgaged || PropertyEffects.frozen(t) ? 0 : PropertyEffects.concertRent(rentWhenActive(t, level),t.concertStacks); }
    /** 输入地产列表与持有人，返回合计主观价值。 */
    private double tileValues(List<Integer> ids, String owner) { return ids.stream().mapToDouble(id -> value(state.board.get(id), owner)).sum(); }
    /** 输入玩家，返回其公开实力对策略的影响权重。 */
    private double threat(PublicPlayerVo p) { return horizon == 1 ? .35 : .4 + .4 * Math.min(2, p.netWorth / (double) Math.max(1, me.netWorth)); }
    /** 输入本人卡牌，根据用途、金额及稀缺性返回保留的机会价值。 */
    private double keepValue(Card c) {
        double base = startIncome() * .25;
        return switch (c.kind) {
            case "cash" -> parameter(c, "amount", 0);
            case "immunity" -> Math.max(base, state.board.stream().filter(t -> t.ownerId != null && !me.id.equals(t.ownerId)).mapToDouble(t -> rent(t, t.level)).max().orElse(0) * .6);
            case "halfPayment", "release", "remote" -> base * 1.5;
            case "freeUpgrade", "fullUpgrade" -> Math.max(base, state.board.stream().filter(t -> me.id.equals(t.ownerId) && !PropertyEffects.frozen(t) && !t.mortgaged && t.level < Rules.MAX_LEVEL).mapToDouble(t -> upgradeValue(t, c.kind.equals("fullUpgrade") ? Rules.MAX_LEVEL : t.level + 1)).max().orElse(0));
            case "fullDemolish" -> Math.max(base, state.board.stream().filter(t -> t.ownerId != null && !me.id.equals(t.ownerId) && t.level > 0).mapToDouble(t -> value(t, t.ownerId) - valueAtLevel(t, t.ownerId, 0)).max().orElse(0));
            default -> base;
        };
    }
    /** 输入已公开的赌博参数，依据期望收益和现金储备决定是否承担下注。 */
    private boolean gamble(ContentResult c) { return bet(parameter(c, "wager", 0), parameter(c, "prize", 0) * parameter(c, "probability", 0) / 100.0); }
    /** 输入本金和期望总返款，返回现金安全且收益合理时是否参与；简单难度允许小幅估值偏差。 */
    private boolean bet(long stake, double expected) { return stake <= me.cash && me.cash - stake >= reserve && expected >= stake * (horizon == 1 ? .9 : 1) && (horizon == 1 || expected > stake || me.cash > reserve + stake * 4); }
    /** 输入已冻结卡面、参数名和旧牌默认值，返回本人的合法卡面参数。 */
    private Long parameter(ContentResult c, String key, long fallback) { return c.parameters.getOrDefault(key, fallback); }
    /** 输入公开活动金额键，返回本局冻结值。 */
    private long money(String key) { return state.activityRotation.money.get(key); }
    /** 无入参，返回本局经过起点的公开收入。 */
    private long startIncome() { return state.economy.money("startIncome", Rules.START_INCOME); }
    /** 输入公开玩家身份，返回对应的白名单玩家。 */
    private PublicPlayerVo person(String id) { return state.players.stream().filter(p -> p.id.equals(id)).findFirst().orElseThrow(); }
    /** 输入操作类型，返回尚未执行的领域意图。 */
    private static Action action(String type) { Action a = new Action(); a.type = type; return a; }
    /** 输入资产操作与地产，返回领域意图。 */
    private static Action tileAction(String type, int tile) { Action a = action(type); a.tileId = tile; return a; }
    /** 输入接收人，返回默认零现金与空地产列表的合法报价容器。 */
    private static Action trade(String to) { Action a = action("tradeOffer"); a.toId = to; a.giveCash = 0L; a.wantCash = 0L; a.giveTiles = List.of(); a.wantTiles = List.of(); a.debtCredit = 0L; return a; }
    /** 输入候选报价，仅与本人已发报价比较，屏蔽同一接收人、条件和资产状态的重复报价。 */
    private boolean repeated(Action action) { return previousQuotes.contains(quoteKey(state, action)); }
    /** 输入安全公开局面和报价，返回与随机数、聊天和房间版本无关的报价条件键。 */
    public static String quoteKey(PublicRoomVo state, Action action) {
        StringBuilder key = new StringBuilder().append(action.toId).append(':').append(action.giveCash).append(':').append(action.wantCash).append(':').append(action.debtCredit);
        for (List<Integer> ids : List.of(action.giveTiles, action.wantTiles)) {
            key.append('|');
            for (int id : ids.stream().sorted().toList()) {
                Tile tile = state.board.get(id);
                key.append(id).append(':').append(tile.ownerId).append(':').append(tile.level).append(':').append(tile.mortgaged).append(':').append(tile.mortgageBase).append(';');
            }
        }
        if (state.pending != null && Set.of("rent", "debt").contains(state.pending.type)) key.append('|').append(state.pending.id).append(':').append(state.pending.amount);
        return key.toString();
    }
}
