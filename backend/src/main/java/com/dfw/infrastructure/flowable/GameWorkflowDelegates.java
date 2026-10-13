package com.dfw.infrastructure.flowable;

import com.dfw.domain.*;
import java.util.Map;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.task.service.delegate.DelegateTask;

/** 通用流程的无状态回调；选择种类通过房间上下文决定。 */
public final class GameWorkflowDelegates {
    private GameWorkflowDelegates() {}
    /** 输入命令作用域，返回仅向 Flowable 表达式开放的三个回调。 */
    public static Map<Object, Object> beans(GameWorkflowScope scope) {
        return Map.of("gameAcceptInput", new AcceptInput(scope), "gameAdvance", new Advance(scope), "gameBindDecision", new BindDecision(scope));
    }
    /** 输入房间，返回包含回合、操作阶段和报价身份的选择上下文。 */
    public static String decisionKey(Room room) {
        if (!room.status.equals("playing")) return room.status;
        PendingDecision pending = room.pending;
        String choice = pending == null ? "" : pending.type + ":" + pending.playerId + ":" + pending.tileId + ":" + pending.amount + ":" + pending.creditorId;
        return room.turnNumber + ":" + GameSupport.current(room).id + ":" + room.phase + ":" + choice + ":" + (room.trade == null ? "" : room.trade.id)
                + (room.runtime != null && room.runtime.ruleSetDigest != null ? ":stage:" + room.runtime.stageIndex : "");
    }
    /** 通用输入节点，复用领域校验而不依赖任务 assignee 进行授权。 */
    public static final class AcceptInput implements JavaDelegate {
        private final GameWorkflowScope scope;
        /** 输入同步命令作用域，构造无状态输入回调。 */
        public AcceptInput(GameWorkflowScope scope) { this.scope = scope; }
        /** 输入流程执行上下文，仅应用本次命令；系统推进没有玩家输入。 */
        @Override public void execute(DelegateExecution execution) {
            var context = scope.context();
            if (context.action() != null) context.engine().apply(context.room(), context.actorId(), context.action());
        }
    }
    /** 展开效果并推进到下一个玩家选择，不在流程图中列举每张卡。 */
    public static final class Advance implements JavaDelegate {
        private final GameWorkflowScope scope;
        /** 输入同步命令作用域，构造无状态结算回调。 */
        public Advance(GameWorkflowScope scope) { this.scope = scope; }
        /** 输入流程执行上下文，结算候选房间并输出等待者与结束标志。 */
        @Override public void execute(DelegateExecution execution) {
            var context = scope.context(); Room room = context.room();
            room.runtime.processInstanceId = execution.getProcessInstanceId(); room.runtime.taskId = null;
            context.engine().advanceUntilWait(room);
            room.runtime.decisionKey = decisionKey(room);
            execution.setVariable("gameOver", !room.status.equals("playing"));
            execution.setVariable("decisionKey", room.runtime.decisionKey);
            execution.setVariable("actorId", room.status.equals("playing") ? (room.trade != null ? room.trade.toId : room.pending != null ? room.pending.playerId : GameSupport.current(room).id) : "");
        }
    }
    /** 绑定等待任务，不额外查询 Flowable 任务表。 */
    public static final class BindDecision implements TaskListener {
        private final GameWorkflowScope scope;
        /** 输入同步命令作用域，构造无状态任务监听器。 */
        public BindDecision(GameWorkflowScope scope) { this.scope = scope; }
        /** 输入创建中的通用任务，把任务标识写入同事务候选快照。 */
        @Override public void notify(DelegateTask task) { scope.context().room().runtime.taskId = task.getId(); }
    }
}
