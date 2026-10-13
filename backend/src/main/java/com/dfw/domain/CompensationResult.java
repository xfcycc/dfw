package com.dfw.domain;

import lombok.Getter;
import lombok.Setter;

/** 一条已匹配灾害补偿订阅的判定与支付进度，仅存内部快照。 */
@Getter
@Setter
public class CompensationResult {
    /** 已匹配的订阅标识。 */
    public String ruleId;
    /** 服务端 0～99 样本；尚未判定时为空。 */
    public Integer sample;
    /** 待支付的补偿金额。 */
    public long amount;
    /** 此订阅已实际支付的金额。 */
    public long paid;
    /** 奖励效果是否已结算，零额奖励也不重复支付或记录。 */
    public boolean settled;
}
