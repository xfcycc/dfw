package com.dfw.domain;

/** 首版经济和人数规则的唯一后端定义。 */
public final class Rules {
    public static final int MIN_PLAYERS = 2, MAX_PLAYERS = 6, INITIAL_CARDS = 2, MAX_LEVEL = 3;
    public static final long INITIAL_CASH = 6000, START_INCOME = 1000, TRADE_TIMEOUT_MS = 60000;
    public static final int[] PRICES = {800, 1200, 1600}, RENT_MULTIPLIERS = {1, 3, 6, 10};
    public static final String[] COLORS = {"#ef6c68", "#4287e5", "#e6ad32", "#47ae88", "#a078d3", "#e982b2"};
    public static final String[] ITEMS = {"remote", "barrier", "stay", "immunity", "demolish", "swap"};
    /** 输入地产，返回抵押和建筑等级对应的租金。 */
    public static long rent(Tile tile) { return tile.mortgaged ? 0 : tile.price / 10 * RENT_MULTIPLIERS[tile.level]; }
    /** 输入地产，返回一级建筑成本。 */
    public static long buildCost(Tile tile) { return tile.price / 2; }
    /** 输入地产，返回出售一级建筑所得。 */
    public static long sellBuildingValue(Tile tile) { return tile.price / 4; }
    /** 输入地产，返回抵押或未抵押卖地所得。 */
    public static long landValue(Tile tile) { return tile.price / 2; }
    /** 输入地产，返回赎回成本。 */
    public static long redeemCost(Tile tile) { return tile.price * 55 / 100; }
}
