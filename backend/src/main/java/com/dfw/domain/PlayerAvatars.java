package com.dfw.domain;

import java.util.Set;

/** 房间可选形象标识，仅影响公开角色外观。 */
public final class PlayerAvatars {
    /** 内置可用形象。 */
    public static final Set<String> IDS = Set.of(
        "explorer", "robot", "wizard", "pirate", "astronaut", "cat",
        "swordsman", "cyberpunk", "elf", "panda", "chef", "rockstar", "mermaid", "dragon", "detective", "bunny"
    );
    /** 兼容缺少外观的旧玩家。 */
    public static final String DEFAULT = "explorer";
    private PlayerAvatars() { }
}
