package com.dfw.domain;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/** 本局通用流程和效果进度，仅保存在房间内部快照。 */
@Getter
@Setter
public class GameRuntime {
    /** 固定引擎标识，空运行时表示旧引擎。 */
    public String engineMode = "FLOWABLE_RUNTIME";
    /** 本局绑定的内置规则版本。 */
    public String ruleSetVersion = "classic-v1";
    /** 配置规则文件摘要；classic-v1 没有摘要并保留旧路径。 */
    public String ruleSetDigest;
    /** 当前阶段所属的回合编号。 */
    public int stageTurnNumber;
    /** 当前版本阶段序列的执行位置。 */
    public int stageIndex;
    /** 行动窗口已接受的退出结果。 */
    public String windowExit;
    /** 当前移动来源，RANDOM 或 REPLACE。 */
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
    /** 本局已完成的效果数量。 */
    public long settledEffects;
}
