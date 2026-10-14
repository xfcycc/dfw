package com.dfw.domain;

import java.util.*;
import com.dfw.domain.rules.RuleSet;
import com.dfw.domain.rules.RuleRuntime;
import static com.dfw.domain.GameError.require;
import static com.dfw.domain.GameSupport.*;

/** 房间生命周期和操作路由；各规则由对应领域服务执行。 */
public class GameEngine {
    public final TurnService turns;
    /** 通用流程驱动的新局效果结算器。 */
    public final EffectResolver effects;
    /** 当前配置的阶段与道具解释器。 */
    public final RuleRuntime ruleRuntime;
    private final RuleSet rules;
    private final EconomyRules economyRules;
    private final MapService maps;
    private final EconomyService economy;
    private final ItemService items;
    private final TradeService trades;
    private final RandomSource random;
    private final ContentService content;
    /** 输入随机源、规则集、经济和地区配置，组装无网络或数据库依赖的游戏规则。 */
    public GameEngine(RandomSource random, RuleSet rules, EconomyRules economyRules, MapRegions regions) {
        this.random = random; this.rules = rules; this.economyRules = economyRules; maps = new MapService(regions); economy = new EconomyService(random);
        effects = new EffectResolver(random);
        content = new ContentService(rules.getContent(), effects, random);
        turns = new TurnService(economy, random, effects, content);
        new EventService(effects, economy, turns, random, rules);
        ruleRuntime = new RuleRuntime(rules, effects, turns, random, content);
        items = new ItemService(effects, ruleRuntime, content); trades = new TradeService(economy, random, effects);
    }
    /** 输入房间，返回是否需由通用流程继续配置阶段。 */
    public boolean needsAdvance(Room room) { return room.runtime != null && ruleRuntime.needsAdvance(room); }
    /** 输入房间，统一按当前配置及已保存效果推进到下一等待点。 */
    public void advanceUntilWait(Room room) { ruleRuntime.advanceUntilWait(room); }
    /** 输入房间，刷新公开扩展说明及大厅的新局金额预览，进行中金额不重算。 */
    public void refreshRuleDescriptions(Room room) {
        if (room.status.equals("lobby")) room.economy = economyRules.snapshot();
        room.chanceEventDescriptions = room.status.equals("lobby") ? new ArrayList<>() : new ArrayList<>(List.of("排名事件按净资产锁定目标，并列随机选择", "免费升级可选择或放弃，确实无合法地产时改给现金", "赌博可放弃，仅使用已有现金；下注后按卡面概率开奖", "灾害按实际建筑损失，以 30% 概率补偿 50%；抵押地产也可能受损"));
    }
    /** 输入同格式已恢复房间，保留基线版本已经进入筹款或协商的债务，不允许按余额再次减免。 */
    public void restoreContentState(Room room) {
        if(paymentPending(room) && room.pending.originalAmount==0) { room.pending.originalAmount=room.pending.amount; room.pending.defenseResolved=true; }
    }
    /** 输入昵称和颜色索引，返回新的大厅玩家。 */
    public Player createPlayer(String name, int index) {
        Player player = new Player(); player.id = random.id(); player.name = name.strip().isEmpty() ? "玩家 " + (index + 1) : name.strip(); player.color = Rules.COLORS[index]; return player;
    }
    /** 输入房间号和首位玩家，返回大厅房间。 */
    public Room createRoom(String code, Player player) { Room room = new Room(); room.code = code; room.hostId = player.id; room.players.add(player); room.economy = economyRules.snapshot(); return room; }
    /** 输入房间，初始化地图、行动顺序、现金及私人手牌。 */
    private void start(Room room) {
        require(room.players.size() >= Rules.MIN_PLAYERS && room.players.size() <= Rules.MAX_PLAYERS, "需要 2～6 名玩家");
        require(room.players.stream().allMatch(p -> p.ready && p.online), "请等待所有玩家连接并准备");
        room.status = "playing"; refreshRuleDescriptions(room); room.gameId = random.id(); room.seed = random.id(); room.economy = economyRules.snapshot(); freezeMoney(room); room.board = maps.generate(room.players.size(), room.mapSize, room.seed, room.economy);
        room.turnOrder = new ArrayList<>(room.players.stream().map(p -> p.id).toList());
        for (int i = room.turnOrder.size() - 1; i > 0; i--) Collections.swap(room.turnOrder, i, random.nextInt(i + 1));
        room.turnIndex = 0; room.turnNumber = 1; room.phase = "pre_roll"; room.pending = null; room.trade = null;
        room.dice = null; room.motion = null; room.activeCardUsed = false; room.eventDrawn = false; room.upgradedTiles.clear();
        for (Player player : room.players) { reset(player); player.cash = room.economy.money("initialCash", Rules.INITIAL_CASH); turns.giveCards(room, player, Rules.INITIAL_CARDS); }
        log(room, "新地图已生成，" + room.players.size() + " 人的财富旅程开始！");
        HistoryTrace.event(room, "game_started", room.hostId, "新对局开始", null);
        HistoryTrace.event(room, "turn_start", room.turnOrder.getFirst(), "首次行动开始", null);
    }
    /** 输入新局房间，将基础项目及现有规则的现金常量换算为固定金额，重启不重算旧基准。 */
    private void freezeMoney(Room room) {
        content.freeze(room);
        room.economy.fixedAmounts.put("initialCash", room.economy.money("initialCash", Rules.INITIAL_CASH));
        room.economy.fixedAmounts.put("startIncome", room.economy.money("startIncome", Rules.START_INCOME));
        for (int i = 0; i < Rules.PRICES.length; i++) room.economy.fixedAmounts.put("landPrice." + i, room.economy.money("landPrice." + i, Rules.PRICES[i]));
        for (var event : rules.getChanceEvents()) freezeSteps(room, event.id(), event.steps());
        for (var event : rules.getFortuneEvents()) freezeSteps(room, event.id(), event.steps());
        for (var card : rules.getCards().values()) freezeSteps(room, card.id(), card.steps());
        for (var stage : rules.getStages()) freezeSteps(room, stage.id(), stage.steps());
    }
    /** 输入房间及规则来源，保存现金常量的最终金额；动态现金表达式不进行倍率换算。 */
    private void freezeSteps(Room room, String scope, List<RuleSet.Step> steps) {
        for (int i = 0; i < steps.size(); i++) if (steps.get(i).cashBase() != null) {
            String key = scope + "." + i + ".amount"; room.economy.fixedAmounts.put(key, room.economy.money(key, steps.get(i).cashBase()));
        }
    }
    /** 输入玩家，重置对局资产与淘汰状态，保留身份与连接。 */
    private void reset(Player player) { player.cash = 0; player.position = 0; player.bankrupt = false; player.skipTurns = 0; player.jailed = false; player.eliminationOrder = null; player.hand.clear(); player.statuses.clear(); player.contentNotice=null; player.contentNoticeText=null; }
    /** 输入房间和身份操作，在候选房间中应用规则，由应用层持久化。 */
    public void apply(Room room, String playerId, Action action) {
        Player player = player(room, playerId); require(player != null, "你的席位已经失效");
        switch (action.type) {
            case "ready" -> { require(room.status.equals("lobby"), "游戏已开始"); player.ready = action.value; return; }
            case "setMapSize" -> {
                require(room.hostId.equals(player.id) && room.status.equals("lobby"), "只有房主可在开局前修改地图大小");
                require(action.mapSize != null && Set.of("small", "medium", "large").contains(action.mapSize), "请选择小、中或大地图");
                room.mapSize = action.mapSize; return;
            }
            case "start" -> { require(room.hostId.equals(player.id) && room.status.equals("lobby"), "只有房主可开始游戏"); start(room); return; }
            case "newGame" -> {
                require(room.hostId.equals(player.id) && room.status.equals("finished"), "只有房主可在结束后再开一局");
                room.status = "lobby"; room.economy = economyRules.snapshot(); room.chanceEventDescriptions.clear(); room.board.clear(); room.gameId = null; room.turnOrder.clear(); room.turnIndex = 0; room.turnNumber = 0;
                room.phase = "pre_roll"; room.pending = null; room.trade = null; room.winnerId = null; room.dice = null; room.motion = null; room.logs.clear();
                for (Player p : room.players) { reset(p); p.ready = false; } return;
            }
            case "removePlayer" -> {
                require(room.hostId.equals(player.id), "只有房主可移出玩家"); require(!room.status.equals("finished"), "请先返回大厅，再管理已结束对局的席位");
                Player target = player(room, action.playerId); require(target != null && !target.id.equals(player.id) && !target.online, "只能移出其他离线玩家");
                if (room.status.equals("lobby")) room.players.remove(target);
                else {
                    require(!target.bankrupt, "该玩家已退出竞争"); boolean isCurrent = current(room) == target;
                    PendingDecision debt = paymentPending(room) && room.pending.playerId.equals(target.id) ? room.pending : null;
                    economy.eliminate(room, target, debt == null ? null : debt.creditorId, debt == null ? 0 : debt.amount);
                    if (room.runtime.globalEvent == null) { if (isCurrent) turns.advance(room); else turns.checkWinner(room); }
                } return;
            }
            default -> { }
        }
        require(room.status.equals("playing") && !player.bankrupt, "你当前不能进行游戏操作"); require(room.pausedReason == null, room.pausedReason == null ? "游戏已暂停" : room.pausedReason);
        if (room.trade != null) { trades.respond(room, player, action); return; }
        require(room.pending == null ? current(room) == player : room.pending.playerId.equals(player.id), "还没有轮到你");
        if (room.runtime.choice != null) {
            require(action.type.equals("contentDecision"), "请先完成当前私人选择"); content.decide(room, player, action); return;
        }
        if (paymentPending(room) && !room.pending.defenseResolved) require(Set.of("paymentDefense", "rentDecision").contains(action.type), "请先选择本笔费用的防御或放弃");
        if (paymentPending(room)) require(Set.of("paymentDefense", "payDebt", "rentDecision", "sellBuilding", "mortgage", "sellLand", "tradeOffer", "declareBankruptcy").contains(action.type), "请先处理当前费用");
        ruleRuntime.validateAction(room, action.type);
        switch (action.type) {
            case "roll" -> {
                require(room.phase.equals("pre_roll"), "本回合已移动");
                room.runtime.rollMode = "RANDOM";
            }
            case "card" -> items.use(room, player, action);
            case "buy", "upgrade" -> {
                require(room.phase.equals(action.type) && room.pending != null && room.pending.tileId != null, "当前没有这项操作");
                Tile tile = room.board.get(room.pending.tileId); long cost = action.type.equals("buy") ? tile.price : Rules.buildCost(room, tile);
                require(player.cash >= cost, "现金不足，可先管理资产或放弃");
                if (action.type.equals("buy")) { require(tile.ownerId == null, "地产已被购买"); tile.ownerId = player.id; log(room, player.name + " 买下 " + tile.name); }
                else { require(Objects.equals(tile.ownerId, player.id) && !tile.mortgaged && tile.level < Rules.MAX_LEVEL && !room.upgradedTiles.contains(tile.id), "这块地产当前不可升级"); tile.level++; room.upgradedTiles.add(tile.id); log(room, player.name + " 将 " + tile.name + " 升至 " + tile.level + " 级"); }
                player.cash -= cost; room.phase = "manage"; room.pending = null;
            }
            case "pass" -> { require(room.phase.equals("buy") || room.phase.equals("upgrade"), "当前没有可放弃的选项"); room.phase = "manage"; room.pending = null; }
            case "paymentDefense" -> items.defense(room, player, action);
            case "rentDecision" -> {
                require(room.phase.equals("rent") && room.pending != null, "当前没有待支付的租金");
                if (!room.pending.defenseResolved) {
                    if (Boolean.TRUE.equals(action.useCard)) action.cardId = player.hand.stream().filter(c -> c.kind.equals("immunity")).map(c -> c.id).findFirst().orElseThrow(() -> new GameError("你没有免租卡"));
                    items.defense(room, player, action);
                } else require(!Boolean.TRUE.equals(action.useCard), "本笔费用已完成防御选择");
                if (!Boolean.TRUE.equals(action.useCard) && room.pending != null) turns.pay(room, player);
            }
            case "payDebt" -> { require(paymentPending(room), "当前没有待支付的费用"); turns.pay(room, player); }
            case "declareBankruptcy" -> { require(paymentPending(room), "当前没有待支付的费用"); turns.declareBankruptcy(room, player); }
            case "sellBuilding", "mortgage", "redeem", "sellLand" -> {
                economy.manage(room, player, action.type, action.tileId);
                if (room.pending != null && room.pending.type.equals("upgrade")) {
                    Tile tile = room.board.get(room.pending.tileId); if (!Objects.equals(tile.ownerId, player.id) || tile.mortgaged || tile.level >= Rules.MAX_LEVEL) { room.pending = null; room.phase = "manage"; }
                }
            }
            case "tradeOffer" -> trades.offer(room, player, action);
            case "endTurn" -> { require(room.phase.equals("manage"), "请先移动并处理当前事项"); }
            default -> throw new GameError("当前操作不可用");
        }
        ruleRuntime.accepted(room, action.type);
    }
}
