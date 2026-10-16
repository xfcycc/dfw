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
    /** 直接传送仅包含目的地，不表示沿途走格；旧快照缺省为否。 */
    public boolean teleport;
    /** 经过路径。 */
    public List<Integer> path = new ArrayList<>();
    /** 本次结算的全部移动片段，旧快照缺省为空；原字段继续表示最后一段。 */
    public List<MotionSegment> segments = new ArrayList<>();
}
