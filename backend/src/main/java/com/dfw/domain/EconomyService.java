package com.dfw.domain;

import java.util.*;
import static com.dfw.domain.GameSupport.*;
import static com.dfw.domain.GameError.require;

/** 地产变现、债务和淘汰规则。 */
public class EconomyService {
    /** 输入房间和玩家，返回现金加全部可变现资产。 */
    public long liquidationValue(Room room, Player player) {
        long value = player.cash;
        for (Tile tile : room.board) if (Objects.equals(tile.ownerId, player.id)) value += tile.level * Rules.sellBuildingValue(tile) + (tile.mortgaged ? 0 : Rules.landValue(tile));
        return value;
    }
    /** 输入房间、被清算人和债权，完成清算但由回合服务切换行动者。 */
    public void eliminate(Room room, Player player, String creditorId, long debt) {
        Player creditor = player(room, creditorId);
        if (creditor != null && !creditor.bankrupt) creditor.cash += Math.min(liquidationValue(room, player), debt);
        for (Tile tile : room.board) if (Objects.equals(tile.ownerId, player.id)) { tile.ownerId = null; tile.level = 0; tile.mortgaged = false; }
        player.cash = 0; player.hand.clear(); player.bankrupt = true; player.skipTurns = 0; player.jailed = false;
        player.eliminationOrder = (int) room.players.stream().filter(p -> p.bankrupt).count();
        if (room.trade != null && (room.trade.fromId.equals(player.id) || room.trade.toId.equals(player.id))) room.trade = null;
        if (room.pending != null && Objects.equals(room.pending.creditorId, player.id)) room.pending.creditorId = null;
        log(room, player.name + " 已清算并退出竞争");
    }
    /** 输入费用与付款人，返回是否发生破产；可偿还时设置债务阶段。 */
    public boolean charge(Room room, Player player, long amount, String creditorId, Integer tileId) {
        if (player.cash >= amount) {
            player.cash -= amount; Player creditor = player(room, creditorId);
            if (creditor != null && !creditor.bankrupt) creditor.cash += amount;
            log(room, player.name + " 支付 " + String.format("%,d", amount) + (creditor != null && !creditor.bankrupt ? " 给 " + creditor.name : " 给银行"));
            room.pending = null; room.phase = "manage";
        } else if (liquidationValue(room, player) < amount) { eliminate(room, player, creditorId, amount); return true; }
        else { room.phase = "debt"; room.pending = pending("debt", player, tileId, amount, creditorId); log(room, player.name + " 需要筹集 " + amount + " 支付费用"); }
        return false;
    }
    /** 输入房间、玩家和地块索引，返回本人地产。 */
    public Tile ownedTile(Room room, Player player, Integer id) {
        Tile tile = tile(room, id); require(tile != null && tile.type.equals("property") && Objects.equals(tile.ownerId, player.id), "这块地产不属于你"); return tile;
    }
    /** 输入经营操作，变更地产与现金，偿债支付由调用者处理。 */
    public void manage(Room room, Player player, String action, int id) {
        require(!room.phase.equals("rent"), "请先处理当前路费");
        require(!room.phase.equals("debt") || !action.equals("redeem"), "偿债阶段不可赎回地产");
        Tile tile = ownedTile(room, player, id);
        switch (action) {
            case "sellBuilding" -> { require(tile.level > 0, "这块地产没有建筑"); tile.level--; player.cash += Rules.sellBuildingValue(tile); log(room, player.name + " 出售 " + tile.name + " 的一级建筑"); }
            case "mortgage" -> { require(tile.level == 0 && !tile.mortgaged, "请先卖完建筑，且不可重复抵押"); tile.mortgaged = true; player.cash += Rules.landValue(tile); log(room, player.name + " 抵押 " + tile.name); }
            case "redeem" -> { require(tile.mortgaged && player.cash >= Rules.redeemCost(tile), "地产未抵押或赎回现金不足"); tile.mortgaged = false; player.cash -= Rules.redeemCost(tile); log(room, player.name + " 赎回 " + tile.name); }
            case "sellLand" -> { require(tile.level == 0, "请先卖完建筑"); if (!tile.mortgaged) player.cash += Rules.landValue(tile); tile.ownerId = null; tile.mortgaged = false; log(room, player.name + " 出售 " + tile.name); }
            default -> throw new GameError("当前操作不可用");
        }
    }
}
