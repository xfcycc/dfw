package com.dfw.domain;

import lombok.Getter;
import lombok.Setter;

/** 私人手牌。 */
@Getter
@Setter
public class Card {
    /** 手牌标识。 */
    public String id;
    /** 道具类型。 */
    public String kind;
}
