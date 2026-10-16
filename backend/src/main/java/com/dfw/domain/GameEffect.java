package com.dfw.domain;

import lombok.Getter;
import lombok.Setter;

/** 可持久化的内置原子效果，不包含 Spring Bean 或执行函数。 */
@Getter
@Setter
public class GameEffect {
    /** 已抽取的完整内容，不重新抽取参数或皮肤。 */
    public ContentResult content;
    /** 本次效果标识。 */
    public String id;
    /** 注册的处理器名称。 */
    public String type;
    /** 效果作用的玩家。 */
    public String playerId;
    /** 可选目标玩家或债权人。 */
    public String targetId;
    /** 可选地产标识。 */
    public Integer tileId;
    /** 移动步数或金额。 */
    public long amount;
    /** 可选公开结果说明。 */
    public String message;
    /** 持续状态定义标识。 */
    public String statusId;
    /** 可选地块选择器 RANDOM_BUILT_PROPERTY。 */
    public String selector;
    /** 补偿成功概率的整数百分比。 */
    public int probability;
    /** 按摧毁前租金快照赔付的整数百分比，旧待结算效果保留原基数。 */
    public int compensationPercent;
    /** 事实触发的子效果所关联的实际损失事件标识。 */
    public String sourceEventId;
    /** 实际匹配的补偿订阅标识。 */
    public String passiveRuleId;
    /** 完整报价，仅用于建立或确认交易。 */
    public Trade trade;
    /** 处理费用等待中的支付、清算或交易，先于暂停的父组合执行。 */
    public boolean resumesDecision;
}
