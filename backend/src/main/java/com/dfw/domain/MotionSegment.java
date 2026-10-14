package com.dfw.domain;

import java.util.*;
import lombok.Getter;
import lombok.Setter;

/** 单次结算中按实际执行顺序保存的公开移动片段。 */
@Getter
@Setter
public class MotionSegment {
    /** 本段路径标识。 */
    public String id;
    /** 本段移动玩家。 */
    public String playerId;
    /** 本段起始位置。 */
    public int from;
    /** 本段实际经过路径，包含路障截停后的终点。 */
    public List<Integer> path = new ArrayList<>();
    /** 仅掷骰产生的第一段填写点数，事件移动为空。 */
    public Integer dice;
    /** 触发本段移动的公开事件，跨选择恢复时先揭卡，私人事件不填写。 */
    public ContentResult beforeEvent;
    /** 本段落点抽出的公开事件卡，私人补给不填写。 */
    public ContentResult event;
}
