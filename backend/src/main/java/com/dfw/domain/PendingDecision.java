package com.dfw.domain;

import lombok.Getter;
import lombok.Setter;

/** 待处理买地、升级、费用或泛化私人选择。 */
@Getter
@Setter
public class PendingDecision {
    /** 翻倍之后、防御之前的原始费用；资产抵债不会据余额再次减免。 */
    public long originalAmount;
    /** 本笔费用已选择防御或放弃，随后才可资产抵债。 */
    public boolean defenseResolved;
    /** 费用标识，供偿债报价绑定；旧待选可在首次报价时补齐。 */
    public String id;
    /** 待处理类型。 */
    public String type;
    /** 操作人。 */
    public String playerId;
    /** 关联地产。 */
    public Integer tileId;
    /** 待支付金额。 */
    public long amount;
    /** 债权人，空表示银行。 */
    public String creditorId;
}
