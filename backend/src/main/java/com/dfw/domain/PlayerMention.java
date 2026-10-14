package com.dfw.domain;

import lombok.Getter;
import lombok.Setter;

/** 公开记录发生当时的玩家标记，不包含私人卡牌或身份令牌。 */
@Getter
@Setter
public class PlayerMention {
    /** 公开玩家身份。 */
    public String id;
    /** 当时的公开昵称。 */
    public String name;
    /** 当时的座位颜色。 */
    public String color;
}
