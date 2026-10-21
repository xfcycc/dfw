package com.dfw.domain;

import java.util.*;
import lombok.Getter;
import lombok.Setter;

/** 八素材扩展的持久化进度；研究选择只通过私人窗口投影。 */
@Getter @Setter
public class NetaState {
    /** 每位施术者至多一份神君标记。 */
    public Map<String, Thunder> thunder = new LinkedHashMap<>();
    /** 持有人的三片攻防共享花瓣。 */
    public Map<String, Petals> petals = new LinkedHashMap<>();
    /** 当前卍解所属的全局回合；跨回合自动失效。 */
    public int bankaiTurn;
    /** 卍解当前持有人。 */
    public String bankaiPlayerId;
    /** 本回合所有后续出牌共用的剩余数量。 */
    public int bankaiRemaining;
    /** 效果身份到各业主已确认的花瓣防御额度；零表示放弃。 */
    public Map<String, Map<String,Integer>> defenses = new LinkedHashMap<>();
    /** 持续花瓣不依赖已经离手的原道具实例。 */
    @Getter @Setter
    public static class Petals {
        /** 攻防共用剩余花瓣，最多三片。 */
        public int charges = 3;
        /** 第三个后续个人回合结束时到期。 */
        public int untilTurn;
        /** 上次主动释放的全局回合。 */
        public int lastAttackTurn;
        /** 冻结的原始卡面，用于禁牌判断及主动防御。 */
        public Card source;
    }
    /** 尚未统一分配的公共研究。 */
    public Research research;

    /** 下一次施术者个人回合开始结算的神君追击。 */
    @Getter @Setter
    public static class Thunder {
        /** 已锁定的目标地产。 */
        public int tileId;
        /** 标记时的业主，转手后不再追击。 */
        public String ownerId;
        /** 施术者触发追击的个人回合。 */
        public int untilTurn;
        /** 当前击数，范围二至四。 */
        public int hits = 2;
        /** 已贡献一次蓄势的玩家。 */
        public Set<String> charged = new HashSet<>();
    }

    /** 公共候选和秘密选择在一个快照内保存，最终分配只执行一次。 */
    @Getter @Setter
    public static class Research {
        /** 已固定的三个公开候选。 */
        public List<Card> cards = new ArrayList<>();
        /** 事件触发时锁定的参与者。 */
        public List<String> participants = new ArrayList<>();
        /** 玩家到候选身份的秘密选择，空字符串代表放弃。 */
        public Map<String, String> selections = new LinkedHashMap<>();
        /** 新版竞标的触发者，额外获得一贡献。 */
        public String triggerId;
        /** 本人秘密投入的零至两张真实手牌身份，仅胜者支付。 */
        public Map<String,List<String>> bids = new LinkedHashMap<>();
        /** 一次抽签固定的胜者；选择奖励前也进入快照。 */
        public String winnerId;
        /** 用于私人窗口的事件卡面。 */
        public ContentResult source;
    }
}
