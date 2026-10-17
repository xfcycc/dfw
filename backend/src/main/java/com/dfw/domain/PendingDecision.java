package com.dfw.domain;

import lombok.Getter;
import lombok.Setter;

/** 待处理买地、升级、费用或泛化私人选择。 */
@Getter
@Setter
public class PendingDecision {
    /** 产生本笔费用的真实公开事件名称，旧待办缺省为空。 */
    public String sourceName;
    /** 本笔费用是否来自角落小游戏，用于累计已结算现金净收益。 */
    public boolean activityPayment;
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
