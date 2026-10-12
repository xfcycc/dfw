package com.dfw.domain;

import lombok.Getter;
import lombok.Setter;

/** 可持久化的内置原子效果，不包含 Spring Bean 或执行函数。 */
@Getter
@Setter
public class GameEffect {
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
    /** 完整报价，仅用于建立或确认交易。 */
    public Trade trade;
    /** 本次效果负责解决正在等待的租金或债务选择。 */
    public boolean resumesDecision;
}
