package com.dfw.domain;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/** 本局通用流程和效果进度，仅保存在房间内部快照。 */
@Getter
@Setter
public class GameRuntime {
    /** 建设扩展的连接、施工机会和多人奖励，旧快照可缺省。 */
    public ConstructionState construction;
    /** 当前阶段所属的回合编号。 */
    public int stageTurnNumber;
    /** 当前配置阶段序列的执行位置。 */
    public int stageIndex;
    /** 行动窗口已接受的退出结果。 */
    public String windowExit;
    /** 当前移动来源，RANDOM、REPLACE 或直接入狱后跳过移动的 JAIL。 */
    public String rollMode;
    /** 已确定但尚未执行的移动步数。 */
    public Integer movementSteps;
    /** Flowable 实例标识。 */
    public String processInstanceId;
    /** 当前通用等待任务标识。 */
    public String taskId;
    /** 当前选择上下文，防止系统推进后继续使用旧任务。 */
    public String decisionKey;
    /** 按实际执行顺序保存的剩余效果。 */
    public List<GameEffect> effects = new ArrayList<>();
    /** 当前组合在原 pending 选择处暂停，剩余步骤尚未执行。 */
    public boolean effectsWaiting;
    /** 暂停中的全图扣款，结束前不切换回合或判断胜负。 */
    public GlobalEventFrame globalEvent;
    /** 最近一次灾害的实际结果与随机样本，仅用于内部恢复和诊断。 */
    public DisasterResult lastDisaster;
    /** 已保存的私人选择及父等待位置。 */
    public ContentChoice choice;
    /** 最近一次事件完整结果，私人补给不进入公开投影。 */
    public ContentResult lastContent;
    /** 最近一次事件的获得者。 */
    public String contentPlayerId;
    /** 最近实际消耗的道具卡面，仅保存可公开的展示字段。 */
    public ContentResult playedCard;
    /** 最近实际使用道具的玩家。 */
    public String playedCardPlayerId;
    /** 本次骰子的加码，只作用于同一次连续移动。 */
    public int rollBonus;
    /** 动漫扩展的回合记录、攻击响应和秘密投票，仅保存在内部快照。 */
    public AnimeState anime = new AnimeState();
    /** 本局已完成的效果数量。 */
    public long settledEffects;
}
