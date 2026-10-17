package com.dfw.domain;

import java.util.Objects;

/** 首版经济和人数规则的唯一后端定义。 */
public final class Rules {
    public static final int MIN_PLAYERS = 2, MAX_PLAYERS = 6, INITIAL_CARDS = 2, MAX_LEVEL = 3, MAX_TILES = 64;
    public static final long INITIAL_CASH = 6000, START_INCOME = 1000;
    public static final int[] PRICES = {400, 600, 800, 1200, 1600}, RENT_MULTIPLIERS = {1, 3, 6, 10};
    public static final String[] COLORS = {"#ef6c68", "#4287e5", "#e6ad32", "#47ae88", "#a078d3", "#e982b2"};
    public static final String[] ITEMS = {"remote", "barrier", "stay", "immunity", "demolish", "swap"};
    /** 输入开局人数，返回应用经济倍率前的每人初始现金基数。 */
    public static long initialCashBase(int count) { return count >= 5 ? 7500 : INITIAL_CASH; }
    /** 输入开局人数，返回每人初始手牌数量。 */
    public static int initialCards(int count) { return count >= 3 ? 3 : INITIAL_CARDS; }
    /** 输入本局玩家人数（含已淘汰玩家），返回每次经过或原地停留在起点时的道具补给数量。 */
    public static int startCards(int count) { return count >= 3 ? 2 : 1; }
    /** 输入人数及房间大小，返回闭环地图总格数；缺失设置沿用大地图。 */
    public static int mapLength(int count, String size) {
        int factor = switch (size == null ? "large" : size) { case "small" -> 4; case "medium" -> 6; case "large" -> 8; default -> throw new GameError("地图大小不可用"); };
        return 16 + count * factor;
    }
    /** 输入房间，返回本局经济快照；旧对局保持原一倍金额。 */
    public static EconomyRules economy(Room room) { return room.economy == null ? new EconomyRules() : room.economy; }
    /** 输入房间及地产，返回按本局基础比例计算的租金；抵押停收，固定金额优先。 */
    public static long rent(Room room, Tile tile) {
        EconomyRules economy = economy(room);
        return tile.mortgaged ? 0 : economy.derived("rent." + tile.price + "." + tile.level, Math.multiplyExact(tile.price, economy.rentBasePercent) / 100 * RENT_MULTIPLIERS[tile.level]);
    }
    /** 输入房间及地产，返回一级建筑成本。 */
    public static long buildCost(Room room, Tile tile) { return economy(room).derived("buildCost." + tile.price, tile.price / 2); }
    /** 输入房间及地产，返回出售一级建筑所得，缺省为实际建设成本的一半。 */
    public static long sellBuildingValue(Room room, Tile tile) { return economy(room).derived("sellBuilding." + tile.price, buildCost(room, tile) / 2); }
    /** 输入房间及地产，返回未抵押空地卖出所得。 */
    public static long landValue(Room room, Tile tile) { return economy(room).derived("sellLand." + tile.price, tile.price / 2); }
    /** 输入房间及地产，返回土地加现有建筑的成本估值。 */
    public static long propertyValue(Room room, Tile tile) { return tile.price + tile.level * buildCost(room, tile); }
    /** 输入房间及地产，返回整体抵押取得的现金。 */
    public static long mortgageValue(Room room, Tile tile) { return propertyValue(room, tile) / 2; }
    /** 输入地产，返回按抵押时估值固定的赎回成本，兼容旧土地抵押。 */
    public static long redeemCost(Tile tile) { return (tile.mortgageBase == null ? tile.price : tile.mortgageBase) * 55 / 100; }
    /** 输入房间及玩家，返回扣除固定赎回费用后的净资产，单块抵押权益最低为零。 */
    public static long netWorth(Room room, Player player) {
        long value = player.cash;
        for (Tile tile : room.board) if (Objects.equals(tile.ownerId, player.id)) value += tile.mortgaged ? Math.max(0, propertyValue(room, tile) - redeemCost(tile)) : propertyValue(room, tile);
        return value;
    }
}
