package com.dfw.domain;

import java.util.Set;
import java.util.Map;

/** 房间可选形象标识，仅影响公开角色外观。 */
public final class PlayerAvatars {
    /** 内置形象及其名称，与前端 avatars.ts 保持一致。 */
    public static final Map<String, String> NAMES = Map.ofEntries(
        Map.entry("explorer", "阿洛"), Map.entry("robot", "波波"), Map.entry("wizard", "露娜"),
        Map.entry("pirate", "杰克"), Map.entry("astronaut", "星野"), Map.entry("cat", "糯米"),
        Map.entry("swordsman", "云舟"), Map.entry("cyberpunk", "霓娜"), Map.entry("elf", "艾琳"),
        Map.entry("panda", "团团"), Map.entry("chef", "小满"), Map.entry("rockstar", "洛琪"),
        Map.entry("mermaid", "海莉"), Map.entry("dragon", "龙焱"), Map.entry("detective", "沈墨"),
        Map.entry("bunny", "米露"), Map.entry("pikachu", "皮卡丘"), Map.entry("eevee", "伊布"),
        Map.entry("ninja", "影丸"), Map.entry("caixukun", "蔡徐坤"),
        Map.entry("su_nuannuan", "苏暖暖"), Map.entry("shana", "夏娜"),
        Map.entry("doraemon", "哆啦A梦"), Map.entry("conan", "柯南"),
        Map.entry("kaito_kid", "怪盗基德"), Map.entry("minato", "波风水门"),
        Map.entry("kakashi", "卡卡西"), Map.entry("ichigo", "黑崎一护"),
        Map.entry("aizen", "蓝染"), Map.entry("su_mucheng", "苏沐橙"),
        Map.entry("goku", "孙悟空"), Map.entry("levi", "利威尔"),
        Map.entry("saitama", "埼玉"), Map.entry("gintoki", "坂田银时"),
        Map.entry("kagura", "神乐"), Map.entry("sakura", "木之本樱"),
        Map.entry("madoka", "鹿目圆"), Map.entry("saber", "Saber"),
        Map.entry("kirito", "桐人"), Map.entry("asuna", "亚丝娜"),
        Map.entry("rem", "雷姆"), Map.entry("aqua", "阿库娅"),
        Map.entry("misaka", "御坂美琴"), Map.entry("accelerator", "一方通行"),
        Map.entry("touma", "上条当麻"), Map.entry("index", "茵蒂克丝"),
        Map.entry("kurumi", "时崎狂三"), Map.entry("tohka", "夜刀神十香"),
        Map.entry("megumi", "加藤惠"), Map.entry("chitoge", "桐崎千棘"),
        Map.entry("inori", "楪祈"), Map.entry("inuyasha", "犬夜叉"),
        Map.entry("kosaki", "小野寺小咲"), Map.entry("kuroyukihime", "黑雪姬"),
        Map.entry("umaru", "土间埋"), Map.entry("edward", "爱德华"),
        Map.entry("susu", "涂山苏苏"), Map.entry("mashiro", "椎名真白"),
        Map.entry("haruhi", "凉宫春日"), Map.entry("yugi", "武藤游戏"),
        Map.entry("monokuma", "黑白熊"), Map.entry("furina", "芙宁娜"),
        Map.entry("paimon", "派蒙"), Map.entry("zhongli", "钟离"),
        Map.entry("keqing", "刻晴"), Map.entry("ayaka", "神里绫华"),
        Map.entry("firefly", "流萤"), Map.entry("jingyuan", "景元"),
        Map.entry("herta", "黑塔"), Map.entry("ruan_mei", "阮·梅"),
        Map.entry("amiya", "阿米娅"), Map.entry("darius", "德莱厄斯"),
        Map.entry("teemo", "提莫"), Map.entry("creeper", "苦力怕"),
        Map.entry("hanli", "韩立"), Map.entry("tangsan", "唐三"),
        Map.entry("xiaowu", "小舞")
    );
    /** 内置可用形象。 */
    public static final Set<String> IDS = NAMES.keySet();
    /** 兼容缺少外观的旧玩家。 */
    public static final String DEFAULT = "explorer";
    private PlayerAvatars() { }
}
