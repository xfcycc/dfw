package com.dfw.interfaces.vo;

import lombok.Getter;
import lombok.Setter;

/** 已冻结的公开个人结算，仅包含实际统计及公开事实，不含内部统计映射。 */
@Getter
@Setter
public class PlayerSettlementVo {
    /** 是否覆盖完整对局。 */
    public boolean complete;
    /** 是否由房主移出。 */
    public boolean removed;
    /** 最高净资产。 */
    public long peakNetWorth;
    /** 同时持有地产的最高数量。 */
    public int peakProperties;
    /** 个人回合次数，包含休息跳过。 */
    public int turns;
    /** 实际现金收租。 */
    public long rentReceived;
    /** 实际现金付租。 */
    public long rentPaid;
    /** 小游戏现金净收益，包含本金及退款。 */
    public long activityNet;
    /** 累计建成的建筑级数。 */
    public int builtLevels;
    /** 成交交易次数。 */
    public int trades;
    /** 实际入狱次数。 */
    public int jailCount;
    /** 结算后的净资产。 */
    public long finalNetWorth;
    /** 实际数据触发的称号。 */
    public String title;
    /** 称号评语。 */
    public String titleQuote;
    /** 真实清算原因或胜利结果。 */
    public String cause;
    /** 对应结果的文案。 */
    public String causeQuote;
    /** 有实际记录的名场面，无记录为空。 */
    public String highlight;
    /** 名场面评语。 */
    public String highlightQuote;
}
