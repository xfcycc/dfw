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
        effects.register("OPEN_TRADE", (room, effect) -> { validate(room, effect.trade); room.trade = effect.trade; });
        effects.register("ACCEPT_TRADE", (room, effect) -> { exchange(room, effect.trade); room.trade = null; });
        effects.register("CLOSE_TRADE", (room, effect) -> { if (effect.message != null) log(room, effect.message); room.trade = null; });
    }
    /** 输入报价，复核双方身份、现金与全部地产，不改变状态。 */
    public void validate(Room room, Trade trade) {
        Player from = player(room, trade.fromId), to = player(room, trade.toId);
        require(from != null && to != null && !from.id.equals(to.id) && !from.bankrupt && !to.bankrupt, "交易对象不可用");
        require(from.cash >= trade.giveCash && to.cash >= trade.wantCash, "交易现金不足");
        require(trade.giveCash > 0 || trade.wantCash > 0 || !trade.giveTiles.isEmpty() || !trade.wantTiles.isEmpty(), "请选择交易内容");
        Set<Integer> unique = new HashSet<>(trade.giveTiles); unique.addAll(trade.wantTiles);
        require(unique.size() == trade.giveTiles.size() + trade.wantTiles.size(), "交易地产不能重复");
        for (int id : trade.giveTiles) economy.ownedTile(room, from, id);
        for (int id : trade.wantTiles) economy.ownedTile(room, to, id);
    }
    /** 输入发起人和报价参数，建立一份私人待确认交易。 */
    public void offer(Room room, Player player, Action action) {
        require(room.phase.equals("pre_roll") || room.phase.equals("manage"), "请先处理当前地块或债务");
        Trade trade = new Trade(); trade.id = random.id(); trade.fromId = player.id; trade.toId = action.toId;
        trade.giveCash = action.giveCash; trade.wantCash = action.wantCash;
        trade.giveTiles = new ArrayList<>(action.giveTiles); trade.wantTiles = new ArrayList<>(action.wantTiles); trade.remainingMs = Rules.TRADE_TIMEOUT_MS;
        validate(room, trade);
        if (room.runtime != null) { GameEffect effect = effects.effect("OPEN_TRADE", player); effect.trade = trade; effects.submit(room, effect); }
        else room.trade = trade;
    }
    /** 输入报价回应者和操作，取消、拒绝或一次性交换全部资产。 */
    public void respond(Room room, Player player, Action action) {
        Trade trade = room.trade;
        if (action.type.equals("tradeCancel")) require(trade.fromId.equals(player.id), "只有发起人可取消报价");
        else if (action.type.equals("tradeRespond")) {
            require(trade.toId.equals(player.id), "只有接收人可回应报价");
            if (action.accept) {
                if (room.runtime != null) { GameEffect effect = effects.effect("ACCEPT_TRADE", player); effect.trade = trade; effects.submit(room, effect); return; }
                exchange(room, trade);
            } else if (room.runtime == null) log(room, "交易报价已拒绝");
        } else throw new GameError("请先完成或取消当前交易");
        if (room.runtime != null) {
            GameEffect effect = effects.effect("CLOSE_TRADE", player); effect.message = action.type.equals("tradeRespond") ? "交易报价已拒绝" : null; effects.submit(room, effect); return;
        }
        room.trade = null;
    }
    /** 输入完整报价，再次校验后一次性交换现金、地产及随地产保留的状态。 */
    private void exchange(Room room, Trade trade) {
        validate(room, trade); Player from = player(room, trade.fromId), to = player(room, trade.toId);
        from.cash += trade.wantCash - trade.giveCash; to.cash += trade.giveCash - trade.wantCash;
        for (int id : trade.giveTiles) room.board.get(id).ownerId = to.id;
        for (int id : trade.wantTiles) room.board.get(id).ownerId = from.id;
        log(room, from.name + " 与 " + to.name + " 完成交易：现金 " + trade.giveCash + " / " + trade.wantCash + "，地产 " + names(room, trade.giveTiles) + " / " + names(room, trade.wantTiles));
    }
    /** 输入地产索引，返回公开交易记录中的地标名称。 */
    private String names(Room room, List<Integer> ids) { return ids.isEmpty() ? "无" : ids.stream().map(id -> room.board.get(id).name).collect(Collectors.joining("、")); }
}
