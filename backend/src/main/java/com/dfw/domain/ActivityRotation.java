package com.dfw.domain;

import java.util.*;
import lombok.Getter;
import lombok.Setter;

/** 新局活动角轮换快照，金额为开局冻结的最终值。 */
@Getter
@Setter
public class ActivityRotation {
    /** 新局启用同类别趣味变体，旧快照缺省为关闭。 */
    public boolean variantsEnabled;
    /** 是否已切换为九种具体玩法的洗牌队列；旧快照缺省为关闭，生成后续活动时转换。 */
    public boolean nineGameQueueEnabled;
    /** 开局人数乘五的固定周期。 */
    public int period;
    /** 已完整结束的全局回合数。 */
    public int completedTurns;
    /** 已发生的轮换次数，供客户端去重动画。 */
    public int revision;
    /** 两个活动角的固定索引。 */
    public List<Integer> tileIds = new ArrayList<>();
    /** 当前两个角的玩法。 */
    public List<String> current = new ArrayList<>();
    /** 下一期两个角的玩法。 */
    public List<String> next = new ArrayList<>();
    /** 尚未发出的洗牌队列，不公开。 */
    public List<String> queue = new ArrayList<>();
    /** 本局活动金额，不再次乘倍率。 */
    public Map<String, Long> money = new LinkedHashMap<>();
}
