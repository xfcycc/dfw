package com.dfw.domain;

import java.util.*;
import static com.dfw.domain.GameError.require;

/** 与原 TypeScript 种子算法一致的随机地图生成。 */
public class MapService {
    private static final String[][] NAMES = {
  {"樱花小院", "松果公寓", "向日葵里", "绿荫街", "丁香花园", "枫叶居", "银杏巷", "月季小筑", "香草庭院", "白桦公馆", "云朵社区", "橙花别院"},
  {"海风码头", "蓝湾公寓", "珊瑚街", "落日海岸", "灯塔小屋", "帆船港", "贝壳广场", "浪花酒店", "沙滩别墅", "海盐小镇", "鲸鱼湾", "星沙码头"},
  {"星光百货", "音乐广场", "银河大厦", "霓虹街", "摩天中心", "彩虹影城", "金橙商厦", "云端酒店", "水晶大道", "都会公馆", "天际广场", "极光中心"},
  {"青石小巷", "老街茶馆", "钟楼广场", "红砖公寓", "榕树街", "河畔书屋", "南风客栈", "石桥街", "梧桐院", "春风集市", "百年戏院", "古城公馆"},
    };
    /** 输入人数和种子，返回新的闭环地图。 */
    public List<Tile> generate(int count, String seed) {
        require(count >= Rules.MIN_PLAYERS && count <= Rules.MAX_PLAYERS, "地图人数必须为 2～6 人");
        SeededRandom random = new SeededRandom(seed);
        int length = 16 + count * 4, quarter = length / 4;
        List<Tile> board = new ArrayList<>();
        for (int i = 0; i < length; i++) { Tile tile = new Tile(); tile.id = i; tile.district = i / quarter; board.add(tile); }
        String[] types = {"start", "chance", "fortune", "jail"}, names = {"起点", "机会", "财富", "监狱"};
        for (int i = 0; i < 4; i++) { board.get(i * quarter).type = types[i]; board.get(i * quarter).name = names[i]; }
        int chanceSide = random.next() < .5 ? 0 : 2, fortuneSide = random.next() < .5 ? 1 : 3;
        for (int i = 0; i < 2; i++) {
            Tile tile = board.get((i == 0 ? chanceSide : fortuneSide) * quarter + 2 + (int)(random.next() * (quarter - 3)));
            tile.type = i == 0 ? "chance" : "fortune"; tile.name = i == 0 ? "机会" : "财富";
        }
        for (int side = 0; side < 4; side++) {
            List<Tile> properties = new ArrayList<>();
            for (Tile tile : board) if (tile.district == side && tile.type.equals("property")) properties.add(tile);
            List<String> landmarks = new ArrayList<>(Arrays.asList(NAMES[side])); shuffle(landmarks, random);
            List<Integer> prices = new ArrayList<>();
            for (int i = 0; i < properties.size(); i++) prices.add(Rules.PRICES[i % 3]);
            shuffle(prices, random);
            for (int i = 0; i < properties.size(); i++) { properties.get(i).name = landmarks.get(i); properties.get(i).price = prices.get(i); }
        }
        return board;
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
