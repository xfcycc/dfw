package com.dfw.domain;

import java.util.*;
import static com.dfw.domain.GameSupport.*;
import static com.dfw.domain.GameError.require;

/** 地产变现、债务和淘汰规则。 */
public class EconomyService {
    private final RandomSource random;
    /** 输入随机源，用于固定每笔待支付费用的身份。 */
    public EconomyService(RandomSource random) { this.random = random; }
    /** 输入房间和玩家，返回现金加全部可变现资产。 */
    public long liquidationValue(Room room, Player player) {
        long value = player.cash;
        for (Tile tile : room.board) if (Objects.equals(tile.ownerId, player.id) && !tile.mortgaged) value += Rules.mortgageValue(room, tile);
        return value;
    }
    /** 输入房间、被清算人和债权，完成清算但由回合服务切换行动者。 */
    public void eliminate(Room room, Player player, String creditorId, long debt) {
        Player creditor = player(room, creditorId);
        if (creditor != null && !creditor.bankrupt) creditor.cash += Math.min(liquidationValue(room, player), debt);
        for (Tile tile : room.board) if (Objects.equals(tile.ownerId, player.id)) { tile.ownerId = null; tile.level = 0; tile.mortgaged = false; tile.mortgageBase = null; }
        player.cash = 0; player.hand.clear(); player.statuses.clear(); player.bankrupt = true; player.skipTurns = 0; player.jailed = false;
        player.eliminationOrder = (int) room.players.stream().filter(p -> p.bankrupt).count();
        if (room.trade != null && (room.trade.fromId.equals(player.id) || room.trade.toId.equals(player.id))) room.trade = null;
        if (room.pending != null && Objects.equals(room.pending.playerId, player.id)) room.pending = null;
        if (room.pending != null && Objects.equals(room.pending.creditorId, player.id)) { room.pending.creditorId = null; room.trade = null; }
        log(room, player.name + " 已清算并退出竞争");
    }
    /** 输入强制费用及付款人，保存支付选择；零费用直接完成，不自动扣款或判破产。 */
    public void charge(Room room, Player player, long amount, String creditorId, Integer tileId) {
        require(amount >= 0 && room.pending == null, "费用结算上下文不可用");
        if (amount == 0) return;
        String type = tileId != null && creditorId != null ? "rent" : "debt";
        room.pending = pending(type, player, tileId, amount, creditorId); room.pending.id = random.id(); room.phase = type;
        log(room, player.name + " 待支付 " + String.format("%,d", amount) + "，请选择支付方式");
    }
    /** 输入付款人，确认用现金支付当前费用余额并恢复父流程。 */
    public void pay(Room room, Player player) {
        require(paymentPending(room) && room.pending.playerId.equals(player.id), "当前没有你的待支付费用");
        PendingDecision debt = room.pending;
        require(player.cash >= debt.amount, "现金不足，请先抵押资产或协商抵债");
        player.cash -= debt.amount; Player creditor = player(room, debt.creditorId);
        if (creditor != null && !creditor.bankrupt) creditor.cash += debt.amount;
        log(room, player.name + " 支付 " + String.format("%,d", debt.amount) + (creditor != null && !creditor.bankrupt ? " 给 " + creditor.name : " 给银行"));
        room.pending = null; room.phase = "manage";
    }
    /** 输入债务人，主动结束资不抵债的结算；银行费用须先自行抵押可用地产。 */
    public void declareBankruptcy(Room room, Player player) {
        require(paymentPending(room) && room.pending.playerId.equals(player.id), "当前没有你的待支付费用");
        PendingDecision debt = room.pending;
        require(liquidationValue(room, player) < debt.amount, "现金加可变现资产足以偿清，请选择资产筹款并支付");
        require(debt.creditorId != null || room.board.stream().noneMatch(value -> Objects.equals(value.ownerId, player.id) && !value.mortgaged), "系统费用须先自行抵押全部可用地产");
        eliminate(room, player, debt.creditorId, debt.amount);
    }
    /** 输入房间、玩家和地块索引，返回本人地产。 */
    public Tile ownedTile(Room room, Player player, Integer id) {
        Tile tile = tile(room, id); require(tile != null && tile.type.equals("property") && Objects.equals(tile.ownerId, player.id), "这块地产不属于你"); return tile;
    }
    /** 输入经营操作，变更地产与现金；强制费用始终保留到手动确认支付。 */
    public void manage(Room room, Player player, String action, int id) {
        require(!paymentPending(room) || !action.equals("redeem"), "费用结算期间不可赎回地产");
        Tile tile = ownedTile(room, player, id);
        switch (action) {
            case "sellBuilding" -> { require(tile.level > 0 && !tile.mortgaged, "地产没有可出售建筑，或建筑已随地产抵押"); tile.level--; player.cash += Rules.sellBuildingValue(room, tile); log(room, player.name + " 出售 " + tile.name + " 的一级建筑"); }
            case "mortgage" -> { require(!tile.mortgaged, "不可重复抵押"); tile.mortgageBase = Rules.propertyValue(room, tile); tile.mortgaged = true; long cash = Rules.mortgageValue(room, tile); player.cash += cash; log(room, player.name + " 整体抵押 " + tile.name + "，取得 " + cash + "，赎回需 " + Rules.redeemCost(tile)); }
            case "redeem" -> { require(tile.mortgaged && player.cash >= Rules.redeemCost(tile), "地产未抵押或赎回现金不足"); player.cash -= Rules.redeemCost(tile); tile.mortgaged = false; tile.mortgageBase = null; log(room, player.name + " 赎回 " + tile.name); }
            case "sellLand" -> { require(tile.level == 0, "请先卖完建筑"); if (!tile.mortgaged) player.cash += Rules.landValue(room, tile); tile.ownerId = null; tile.mortgaged = false; tile.mortgageBase = null; log(room, player.name + " 出售 " + tile.name); }
            default -> throw new GameError("当前操作不可用");
        }
    }
}
