package com.dfw.domain;

import java.util.*;
import java.util.stream.Collectors;
import static com.dfw.domain.GameError.require;
import static com.dfw.domain.GameSupport.*;

/** 现金与地产的双方报价和原子交换规则。 */
public class TradeService {
    private final EconomyService economy;
    private final RandomSource random;
    private final EffectResolver effects;
    /** 输入资产校验服务和随机源，构造交易规则。 */
    public TradeService(EconomyService economy, RandomSource random, EffectResolver effects) {
        this.economy = economy; this.random = random; this.effects = effects;
        effects.register("OPEN_TRADE", (room, effect) -> {
            validate(room, effect.trade); room.trade = effect.trade;
            player(room, effect.trade.fromId).tradeRejection = null;
            log(room, "log", effect.playerId, player(room, effect.trade.fromId).name + " 向 " + player(room, effect.trade.toId).name + " 发起交易报价，等待回应；" + summary(room, effect.trade));
        });
        effects.register("ACCEPT_TRADE", (room, effect) -> { exchange(room, effect.trade); room.trade = null; });
        effects.register("CLOSE_TRADE", (room, effect) -> {
            if (room.trade.toId.equals(effect.playerId)) player(room, room.trade.fromId).tradeRejection = player(room, room.trade.toId).name + " 拒绝了你的交易报价，本次交易未成交。";
            if (effect.message != null) log(room, "log", effect.playerId, effect.message);
            room.trade = null;
        });
    }
    /** 输入报价，复核双方身份、现金与全部地产，不改变状态。 */
    public void validate(Room room, Trade trade) {
        Player from = player(room, trade.fromId), to = player(room, trade.toId);
        require(from != null && to != null && !from.id.equals(to.id) && !from.bankrupt && !to.bankrupt, "交易对象不可用");
        require(trade.debtCredit >= 0, "抵债金额不可为负数");
        if (trade.debtId != null) {
            require(paymentPending(room) && trade.debtId.equals(room.pending.id) && trade.fromId.equals(room.pending.playerId) && room.pending.creditorId != null, "报价关联的债务已失效");
            require(trade.debtCredit <= room.pending.amount, "抵扣金额不可超过待支付余额");
        } else require(trade.debtCredit == 0, "只有当前玩家债务可以协商抵扣");
        if (trade.debtCredit > 0) require(trade.toId.equals(room.pending.creditorId) && !trade.giveTiles.isEmpty(), "资产抵债须向债权人提供地产");
        require(from.cash >= trade.giveCash && to.cash >= trade.wantCash, "交易现金不足");
        require(trade.giveCash > 0 || trade.wantCash > 0 || !trade.giveTiles.isEmpty() || !trade.wantTiles.isEmpty(), "请选择交易内容");
        Set<Integer> unique = new HashSet<>(trade.giveTiles); unique.addAll(trade.wantTiles);
        require(unique.size() == trade.giveTiles.size() + trade.wantTiles.size(), "交易地产不能重复");
        for (int id : trade.giveTiles) economy.ownedTile(room, from, id);
        for (int id : trade.wantTiles) economy.ownedTile(room, to, id);
    }
    /** 输入发起人和报价参数，建立待确认交易并公开双方及报价明细。 */
    public void offer(Room room, Player player, Action action) {
        boolean debt = paymentPending(room);
        require(room.phase.equals("pre_roll") || room.phase.equals("manage") || debt, "请先处理当前地块");
        require(!debt || room.pending.creditorId != null, "系统费用需通过现金和银行抵押结算");
        Trade trade = new Trade(); trade.id = random.id(); trade.fromId = player.id; trade.toId = action.toId;
        trade.giveCash = action.giveCash; trade.wantCash = action.wantCash;
        trade.giveTiles = new ArrayList<>(action.giveTiles); trade.wantTiles = new ArrayList<>(action.wantTiles);
        trade.debtCredit = action.debtCredit == null ? 0 : action.debtCredit;
        if (debt) { if (room.pending.id == null) room.pending.id = random.id(); trade.debtId = room.pending.id; }
        validate(room, trade);
        GameEffect effect = effects.effect("OPEN_TRADE", player); effect.trade = trade; effect.resumesDecision = debt; effects.submit(room, effect);
    }
    /** 输入报价回应者和操作，取消、拒绝或一次性交换全部资产。 */
    public void respond(Room room, Player player, Action action) {
        Trade trade = room.trade;
        if (action.type.equals("tradeCancel")) require(trade.fromId.equals(player.id), "只有发起人可取消报价");
        else if (action.type.equals("tradeRespond")) {
            require(trade.toId.equals(player.id), "只有接收人可回应报价");
            if (action.accept) { GameEffect effect = effects.effect("ACCEPT_TRADE", player); effect.trade = trade; effect.resumesDecision = paymentPending(room); effects.submit(room, effect); return; }
        } else throw new GameError("请先完成或取消当前交易");
        String response = action.type.equals("tradeRespond") ? player.name + " 拒绝 " + player(room, trade.fromId).name + " 的交易报价" : player.name + " 取消向 " + player(room, trade.toId).name + " 发起的交易报价";
        GameEffect effect = effects.effect("CLOSE_TRADE", player); effect.message = response + "，未成交；" + summary(room, trade); effect.resumesDecision = paymentPending(room); effects.submit(room, effect);
    }
    /** 输入完整报价，原子交换资产与抵扣债务；部分抵扣继续等待原付款人。 */
    private void exchange(Room room, Trade trade) {
        validate(room, trade); Player from = player(room, trade.fromId), to = player(room, trade.toId);
        from.cash += trade.wantCash - trade.giveCash; to.cash += trade.giveCash - trade.wantCash;
        for (int id : trade.giveTiles) room.board.get(id).ownerId = to.id;
        for (int id : trade.wantTiles) room.board.get(id).ownerId = from.id;
        log(room, "log", to.id, to.name + " 同意 " + from.name + " 的交易报价，双方完成交易；" + summary(room, trade));
        if (trade.debtCredit > 0) {
            String debt = paymentDescription(room, room.pending);
            room.pending.amount -= trade.debtCredit;
            log(room, "log", from.id, from.name + " 以地产抵扣 " + debt + " ¥" + String.format("%,d", trade.debtCredit) + "，剩余待支付 ¥" + String.format("%,d", room.pending.amount) + (room.pending.amount == 0 ? "，本笔欠款已结清" : "，仍需另行确认支付"));
            if (room.pending.amount == 0) { room.pending = null; room.phase = "manage"; }
        }
    }
    /** 输入房间和报价，返回双方资产方向与关联欠款的公开明细，不改变状态。 */
    private String summary(Room room, Trade trade) {
        String text = player(room, trade.fromId).name + " 提供现金 ¥" + String.format("%,d", trade.giveCash) + "、地产 " + names(room, trade.giveTiles)
                + "；" + player(room, trade.toId).name + " 提供现金 ¥" + String.format("%,d", trade.wantCash) + "、地产 " + names(room, trade.wantTiles);
        if (trade.debtId != null && paymentPending(room) && Objects.equals(trade.debtId, room.pending.id)) {
            text += "；关联欠款：" + player(room, trade.fromId).name + " 的 " + paymentDescription(room, room.pending) + "，当前欠款 ¥" + String.format("%,d", room.pending.amount)
                    + "，报价约定抵扣 ¥" + String.format("%,d", trade.debtCredit) + "（对应抵扣不转移现金）";
        }
        return text;
    }
    /** 输入地产索引，返回公开交易记录中的地标名称、编号、建筑及抵押状态。 */
    private String names(Room room, List<Integer> ids) {
        return ids.isEmpty() ? "无" : ids.stream().map(id -> { Tile tile = room.board.get(id); return tile.name + "（" + tile.id + "号，" + tile.level + "级" + (tile.mortgaged ? "，已抵押，赎回 ¥" + String.format("%,d", Rules.redeemCost(tile)) : "") + "）"; }).collect(Collectors.joining("、"));
    }
}
