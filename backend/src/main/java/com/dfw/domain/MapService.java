package com.dfw.domain;

import java.util.*;
import static com.dfw.domain.GameError.require;

/** 与原 TypeScript 种子算法一致的随机地图生成。 */
public class MapService {
    private final MapRegions regions;
    /** 输入独立地区配置，地图内每个街区从同一地区池命名。 */
    public MapService(MapRegions regions) { this.regions = regions; }
    /** 输入人数、房间大小、种子及本局金额，返回各边五档地价尽量均衡的矩形闭环地图。 */
    public List<Tile> generate(int count, String size, String seed, EconomyRules economy) {
        require(count >= Rules.MIN_PLAYERS && count <= Rules.MAX_PLAYERS, "地图人数必须为 2～6 人");
        SeededRandom random = new SeededRandom(seed);
        int length = Rules.mapLength(count, size), horizontal = (length + 3) / 4, vertical = length / 4;
        int[] corners = {0, horizontal, horizontal + vertical, horizontal * 2 + vertical};
        List<Tile> board = new ArrayList<>();
        for (int i = 0; i < length; i++) { Tile tile = new Tile(); tile.id = i; tile.district = i < corners[1] ? 0 : i < corners[2] ? 1 : i < corners[3] ? 2 : 3; board.add(tile); }
        String[] types = {"start", "chance", "fortune", "jail"}, names = {"起点", "机会", "财富", "监狱"};
        for (int i = 0; i < 4; i++) { board.get(corners[i]).type = types[i]; board.get(corners[i]).name = names[i]; }
        int specialCount = count <= 3 ? 2 : 3;
        for (int i = 1; i < specialCount; i++) { addSpecial(board, "chance", "机会", random); addSpecial(board, "fortune", "财富", random); }
        List<MapRegions.Region> groups = new ArrayList<>(regions.regions); shuffle(groups, random);
        Map<String, Integer> nameOffsets = new HashMap<>(); Map<String, List<String>> namePools = new HashMap<>();
        for (var group : groups) { List<String> pool = new ArrayList<>(group.names); shuffle(pool, random); namePools.put(group.id, pool); }
        for (int side = 0; side < 4; side++) {
            var group = groups.get(side % groups.size()); List<Tile> properties = new ArrayList<>();
            for (Tile tile : board) if (tile.district == side) { tile.regionId = group.id; tile.regionName = group.name; if (tile.type.equals("property")) properties.add(tile); }
            List<Long> priceTiers = new ArrayList<>();
            for (int i = 0; i < Rules.PRICES.length; i++) priceTiers.add(economy.money("landPrice." + i, Rules.PRICES[i]));
            shuffle(priceTiers, random);
            List<Long> prices = new ArrayList<>();
            for (int i = 0; i < properties.size(); i++) prices.add(priceTiers.get(i % priceTiers.size()));
            shuffle(prices, random); List<String> pool = namePools.get(group.id);
            for (int i = 0; i < properties.size(); i++) {
                int offset = nameOffsets.getOrDefault(group.id, 0); nameOffsets.put(group.id, offset + 1);
                properties.get(i).name = pool.get(offset % pool.size()) + (offset < pool.size() ? "" : "·" + (offset / pool.size() + 1) + "号地段");
                properties.get(i).price = prices.get(i);
            }
        }
        return board;
    }
    /** 输入棋盘、特殊格类型及随机源，从剩余地产中取一格，与已有特殊格保留至少一格间隔。 */
    private void addSpecial(List<Tile> board, String type, String name, SeededRandom random) {
        List<Tile> available = board.stream().filter(t -> t.type.equals("property") && board.get(Math.floorMod(t.id - 1, board.size())).type.equals("property") && board.get((t.id + 1) % board.size()).type.equals("property")).toList();
        Tile tile = available.get((int) (random.next() * available.size())); tile.type = type; tile.name = name;
    }
    /** 输入列表和地图随机源，原地洗牌。 */
    private <T> void shuffle(List<T> values, SeededRandom random) {
        for (int i = values.size() - 1; i > 0; i--) Collections.swap(values, i, (int)(random.next() * (i + 1)));
    }
    /** Java int 溢出与无符号右移复现 FNV / mulberry32。 */
    private static class SeededRandom {
        private int value = (int)2166136261L;
        /** 输入种子，初始化与旧后端一致的散列。 */
        SeededRandom(String seed) { for (int i = 0; i < seed.length(); i++) value = (value ^ seed.charAt(i)) * 16777619; }
        /** 返回 [0,1) 的地图随机数。 */
        double next() {
            value += 0x6d2b79f5;
            int t = (value ^ (value >>> 15)) * (value | 1);
            t ^= t + (t ^ (t >>> 7)) * (t | 61);
            return Integer.toUnsignedLong(t ^ (t >>> 14)) / 4294967296.0;
        }
    }
}
