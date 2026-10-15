package com.dfw.interfaces.vo;

import java.util.*;
import lombok.Getter;
import lombok.Setter;

/** 活动公开投影，只包含允许当前阶段揭晓的信息。 */
@Getter
@Setter
public class ActivityVo {
    /** 活动身份。 */
    public String id;
    /** 当前玩法。 */
    public String kind;
    /** 触发地块。 */
    public int tileId;
    /** 发起玩家。 */
    public String triggerId;
    /** 当前阶段。 */
    public String stage;
    /** 输入轮次。 */
    public int round;
    /** 参与玩家。 */
    public List<String> participants;
    /** 猜拳未淘汰玩家。 */
    public List<String> contenders;
    /** 已提交玩家，不含秘密选择。 */
    public List<String> submitted;
    /** 已公开的赛马选择。 */
    public Map<String, Integer> bets = new LinkedHashMap<>();
    /** 上一轮已揭晓手势。 */
    public Map<String, Integer> revealed = new LinkedHashMap<>();
    /** 下注本金。 */
    public Map<String, Long> wagers;
    /** 已发放奖励。 */
    public Map<String, Long> payouts;
    /** 已公开红包基础金额。 */
    public Map<String, Long> packets = new LinkedHashMap<>();
    /** 已公开幸运加奖。 */
    public Map<String, Long> bonuses = new LinkedHashMap<>();
    /** 当前轮盘操作者。 */
    public String currentPlayerId;
    /** 轮盘对手。 */
    public String opponentId;
    /** 最近开枪人。 */
    public String lastShooterId;
    /** 已开枪次数。 */
    public int shots;
    /** 已揭晓骰子或冠军编号。 */
    public Integer result;
    /** 展示开始时间。 */
    public long startedAt;
    /** 自动展示截止时间。 */
    public Long endsAt;
    /** 已完成经济结算或产生费用。 */
    public boolean settled;
    /** 可公开结果文案。 */
    public String summary;
}
