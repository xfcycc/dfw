package com.dfw.domain;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/** 一次单地块灾害的实际损失和补偿结果，不接受客户端输入。 */
@Getter
@Setter
public class DisasterResult {
    /** 来源效果标识。 */
    public String effectId;
    /** 唯一事实事件标识，补偿与损失由此关联。 */
    public String eventId;
    /** 实际结果类型，仅成功摧毁建筑时产生。 */
    public String type = "BUILDINGS_DESTROYED";
    /** 事实标签，普通拆房不属于此灾害事件。 */
    public String tag = "DISASTER";
    /** 实际目标地块。 */
    public int tileId;
    /** 损失发生时的产权人。 */
    public String ownerId;
    /** 实际摧毁的建筑级数。 */
    public int destroyedLevels;
    /** 按建筑成本计算的实际损失。 */
    public long loss;
    /** 首条订阅的 0～99 样本；尚未判定时为空。 */
    public Integer sample;
    /** 实际补偿金额。 */
    public long compensation;
    /** 订阅匹配及判定明细，保存样本和已赔金额以支持恢复诊断。 */
    public List<CompensationResult> compensations = new ArrayList<>();
}
