package com.dfw.domain;

import java.util.*;
import lombok.Getter;
import lombok.Setter;

/** 用于前端动画的公开移动路径。 */
@Getter
@Setter
public class Motion {
    /** 移动标识。 */
    public String id;
    /** 移动玩家。 */
    public String playerId;
    /** 起始位置。 */
    public int from;
    /** 经过路径。 */
    public List<Integer> path = new ArrayList<>();
}
