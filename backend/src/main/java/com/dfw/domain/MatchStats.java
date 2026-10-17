package com.dfw.domain;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

/** 随房间存档的个人战报；退出竞争后冻结，不记录私人手牌。 */
@Getter
@Setter
public class MatchStats {
    /** 是否从新局开局起完整统计。 */
    public boolean complete;
    /** 统计开始时的净资产，用于判断实际增长。 */
    public long initialWorth;
    /** 观察到的最高净资产。 */
    public long peakNetWorth;
    /** 同时持有地产的最高数量。 */
    public int peakProperties;
    /** 经历的个人回合，含因监禁或休息跳过的回合。 */
    public int turns;
    /** 已计入个人回合的全局行动编号，防止重复累计。 */
    public int lastTurn;
    /** 实际现金收租，不含地产抵债。 */
    public long rentReceived;
    /** 实际现金付租，不含地产抵债。 */
    public long rentPaid;
    /** 角落小游戏的已结算现金净收益，含本金和中止退款。 */
    public long activityNet;
    /** 实际建成的累计建筑级数，包含免费升级。 */
    public int builtLevels;
    /** 双方确认成交的次数。 */
    public int trades;
    /** 实际施加监禁的次数，解控成功不计。 */
    public int jailCount;
    /** 按付款玩家汇总的实际现金租金。 */
    public Map<String, Long> rentByPayer = new LinkedHashMap<>();
    /** 最大单笔实际现金租金。 */
    public long largestRent;
    /** 最大单笔租金对应的地产说明。 */
    public String largestRentTile;
    /** 已冻结个人结算。 */
    public boolean settled;
    /** 是否由房主移出，区别于主动破产。 */
    public boolean removed;
    /** 冻结时净资产；破产者在清算后为零。 */
    public long finalNetWorth;
    /** 数据触发的个人称号。 */
    public String title;
    /** 与称号匹配的评语。 */
    public String titleQuote;
    /** 真实清算原因或胜利结果。 */
    public String cause;
    /** 与结果匹配的梗文案。 */
    public String causeQuote;
    /** 一条有事实依据的名场面。 */
    public String highlight;
    /** 名场面的评语。 */
    public String highlightQuote;
}
