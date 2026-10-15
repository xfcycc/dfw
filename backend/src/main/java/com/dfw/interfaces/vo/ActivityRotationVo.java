package com.dfw.interfaces.vo;

import java.util.*;
import lombok.Getter;
import lombok.Setter;

/** 活动轮换公开信息，不包含未发出的随机队列。 */
@Getter
@Setter
public class ActivityRotationVo {
    /** 固定周期。 */
    public int period;
    /** 已结束回合数。 */
    public int completedTurns;
    /** 轮换版本。 */
    public int revision;
    /** 两个活动角索引。 */
    public List<Integer> tileIds;
    /** 当期玩法。 */
    public List<String> current;
    /** 下期玩法。 */
    public List<String> next;
    /** 冻结的活动实际金额。 */
    public Map<String, Long> money;
}
