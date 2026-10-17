package com.dfw.domain;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/** 全体扣款执行帧，目标、下一位置和返回阶段随等待点保存。 */
@Getter
@Setter
public class GlobalEventFrame {
    /** 产生全体费用的公开事件名称，旧待结算批次可为空。 */
    public String sourceName;
    /** 发动效果标识。 */
    public String effectId;
    /** 发动时按座位固定的未破产玩家。 */
    public List<String> targets = new ArrayList<>();
    /** 下一个待执行目标；当前偿债目标由 pending 单独保存。 */
    public int nextTarget;
    /** 本批次每人的固定费用。 */
    public long amount;
    /** 全部目标结算后恢复的阶段。 */
    public String returnPhase;
}
