package com.dfw.domain;

import lombok.Getter;
import lombok.Setter;

/** 一次单地块灾害的实际损失和补偿结果，不接受客户端输入。 */
@Getter
@Setter
public class DisasterResult {
    /** 来源效果标识。 */
    public String effectId;
    /** 实际目标地块。 */
    public int tileId;
    /** 损失发生时的产权人。 */
    public String ownerId;
    /** 实际摧毁的建筑级数。 */
    public int destroyedLevels;
    /** 按建筑成本计算的实际损失。 */
    public long loss;
    /** 服务端生成的 0～99 样本，空建筑不抽样。 */
    public int sample;
    /** 实际补偿金额。 */
    public long compensation;
}
