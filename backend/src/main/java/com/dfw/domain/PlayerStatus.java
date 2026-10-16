package com.dfw.domain;

import lombok.Getter;
import lombok.Setter;

/** 持续状态实例，随玩家快照持久化，按持有人的个人回合结束计时。 */
@Getter
@Setter
public class PlayerStatus {
    /** 固定状态定义：TRAP_IMMUNITY、NEXT_PAYMENT_WAIVER、NEXT_RENT_DOUBLE、NEXT_MOVE_DOUBLE。 */
    public String type;
    /** 产生状态的效果标识。 */
    public String sourceEffectId;
    /** 最近施放或刷新时的全局回合号，该回合不扣期限。 */
    public int appliedTurn;
    /** 免疫剩余个人回合数；一次性状态为 1，消费前不按回合到期。 */
    public int remainingTurns;
}
