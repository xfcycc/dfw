package com.dfw.domain;

import java.util.*;
import lombok.Getter;
import lombok.Setter;

/** 动漫扩展的内部持久化进度；秘密投票及响应决策不得进入公开状态。 */
@Getter @Setter
public class AnimeState {
    /** 已初始化个人回合的全局编号。 */
    public int startedTurn;
    /** 当前普通骰点已经抽样，重连不得重新掷骰。 */
    public boolean rollPrepared;
    /** 已开放时间溯行的全局回合。 */
    public int rewindOfferedTurn;
    /** 当前回合实际使用的主动牌完整记录。 */
    public List<Card> currentCards = new ArrayList<>();
    /** 上一个已结束回合的主动牌，投影只读该集合。 */
    public List<Card> previousCards = new ArrayList<>();
    /** 已消费来源牌的攻击响应帧。 */
    public Attack attack;
    /** 学级裁判固定参与人。 */
    public List<String> voters = new ArrayList<>();
    /** 秘密票，仅揭晓时公开统计，不投影此集合。 */
    public Map<String,String> votes = new LinkedHashMap<>();
    /** 一张来源牌的全部固定分段及一次性响应。 */
    @Getter @Setter
    public static class Attack {
        /** 已消费的来源卡。 */
        public Card card;
        /** 原施术者身份。 */
        public String actorId;
        /** 已固定的原子分段，响应后只修改对应接收者。 */
        public List<GameEffect> parts = new ArrayList<>();
        /** 顺序响应者，每人至多一次。 */
        public List<String> defenders = new ArrayList<>();
        /** 下一响应下标。 */
        public int nextDefender;
        /** 各人的选择，空字符串表示放弃。 */
        public Map<String,String> defenses = new LinkedHashMap<>();
        /** 固定分段是否已经入队，恢复时不能重复提交。 */
        public boolean queued;
        /** 是否为可被库洛牌收服的单目标道具。 */
        public boolean singleTarget;
        /** 原阶段，用于结束响应后返回。 */
        public String returnPhase;
    }
}
