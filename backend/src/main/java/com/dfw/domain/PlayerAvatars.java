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
        Map.entry("ninja", "影丸"), Map.entry("caixukun", "蔡徐坤")
    );
    /** 内置可用形象。 */
    public static final Set<String> IDS = NAMES.keySet();
    /** 兼容缺少外观的旧玩家。 */
    public static final String DEFAULT = "explorer";
    private PlayerAvatars() { }
}
