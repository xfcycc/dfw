package com.dfw.domain;

import java.util.*;
import lombok.Getter;
import lombok.Setter;

/** 素材扩展的持久化进度；赌局和候选只通过私人选择投影。 */
@Getter @Setter
public class CrossoverState {
    /** 已确认的双喷回合，每次普通移动只开启一次。 */
    public int boostTurn;
    /** 本次双喷剩余喷射次数。 */
    public int boosts;
    /** 路障已经终止喷射，陨星不会设置此标记。 */
    public boolean boostBlocked;
    /** 回合结束净化机会已开放的全局回合。 */
    public int endPreparedTurn;
    /** 腐蚀和结丹奖励已经执行的全局回合。 */
    public int endAppliedTurn;
    /** 已扣除休息次数、尚在回合结束选择中等待的全局回合。 */
    public int skippedTurn;
    /** 闭关或蓄力放弃主动移动的全局回合。 */
    public int stationaryTurn;
    /** 本局已经结丹的玩家，复制或交换不重置。 */
    public Set<String> cultivated = new HashSet<>();
    /** 等待本回合结束发放结丹奖励的玩家。 */
    public String corePlayerId;
    /** 蓄力牌及可释放的本人个人回合。 */
    public Map<String, Card> chargedCards = new HashMap<>();
    /** 蓄力可释放的个人回合数。 */
    public Map<String, Integer> chargeTurns = new HashMap<>();
    /** 本次蓄力释放窗口已处理的全局回合。 */
    public int chargeOfferedTurn;
    /** 正在结算的损坏来源，仅供记录同一次攻击或灾害。 */
    public String damageBatch;
    /** 主动损坏来源的玩家；公共事件为空。 */
    public String damageActorId;
    /** 每名业主最近一次真实建筑损坏的来源。 */
    public Map<String, String> lastDamageBatch = new HashMap<>();
    /** 同一次建筑损坏前等级、产权和修复期限。 */
    public Map<Integer, Damage> damage = new HashMap<>();
    /** 当前秘密手牌赌局，结束后清空。 */
    public Duel duel;

    /** 仅保存修复需要的损坏证据，不回滚经济操作。 */
    @Getter @Setter
    public static class Damage {
        /** 损坏时业主。 */
        public String ownerId;
        /** 已固定的攻击或灾害身份。 */
        public String batch;
        /** 该来源首次损坏前等级。 */
        public int level;
        /** 可修复到的业主个人回合。 */
        public int untilTurn;
    }

    /** 双方暗押牌及固定骰点，绝不直接公开投影。 */
    @Getter @Setter
    public static class Duel {
        /** 发起人。 */
        public String actorId;
        /** 被邀请的玩家。 */
        public String targetId;
        /** 发起人已锁定的牌，仍保留到双方确认。 */
        public String actorCardId;
        /** 双方确认后一次性固定的发起人骰点。 */
        public Integer actorDie;
        /** 双方确认后一次性固定的受邀人骰点。 */
        public Integer targetDie;
    }
}
