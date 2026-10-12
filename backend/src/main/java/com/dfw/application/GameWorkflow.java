package com.dfw.application;

import com.dfw.domain.*;
import com.dfw.domain.rules.RuleCatalog;
import com.dfw.infrastructure.flowable.GameWorkflowDelegates;
import com.dfw.infrastructure.flowable.GameWorkflowScope;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.flowable.engine.ProcessEngine;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** 将原前端命令适配成通用流程输入，新开局启用，旧快照保留原规则路径。 */
@Component
public class GameWorkflow {
    private static final String PROCESS_KEY = "genericGameRuntime";
    private static final Set<String> LIFECYCLE = Set.of("ready", "start", "newGame", "removePlayer");
    private final ProcessEngine process;
    private final GameWorkflowScope scope;
    private final RuleCatalog rules;
    /** 输入嵌入式流程引擎和候选作用域，构造同步适配器。 */
    public GameWorkflow(ProcessEngine process, GameWorkflowScope scope, RuleCatalog rules) { this.process = process; this.scope = scope; this.rules = rules; }
    /** 输入恢复的全部房间，批量校验任务绑定，避免恢复时逐房间查询数据库。 */
    public void verifyRecovery(List<Room> rooms) {
        List<Room> active = rooms.stream().filter(r -> r.runtime != null && r.status.equals("playing")).toList();
        if (active.isEmpty()) return;
        Map<String, Task> tasks = process.getTaskService().createTaskQuery().processDefinitionKey(PROCESS_KEY).includeProcessVariables().list().stream()
                .collect(Collectors.toMap(Task::getId, Function.identity()));
        for (Room room : active) {
            if (rules.configured(room)) rules.bound(room);
            Task task = tasks.get(room.runtime.taskId);
            if (!"FLOWABLE_RUNTIME".equals(room.runtime.engineMode)
                    || task == null || !Objects.equals(task.getProcessInstanceId(), room.runtime.processInstanceId)
                    || !Objects.equals(task.getProcessVariables().get("gameId"), room.gameId)
                    || !Objects.equals(task.getProcessVariables().get("roomCode"), room.code)
                    || !Objects.equals(task.getProcessVariables().get("decisionKey"), room.runtime.decisionKey)
                    || !Objects.equals(task.getProcessVariables().get("ruleSetVersion"), room.runtime.ruleSetVersion)) {
                throw new IllegalStateException("房间 " + room.code + " 的流程快照不一致，须恢复同一数据库备份");
            }
        }
    }
    /** 输入候选房间、规则、玩家及原协议命令，在外层事务内推进一次通用任务。 */
    public void apply(Room room, GameEngine engine, String actorId, Action action) {
        requireTransaction(); refresh(room, engine);
        if (room.runtime != null && room.status.equals("playing") && !LIFECYCLE.contains(action.type)) {
            complete(room, engine, actorId, action); return;
        }
        engine.apply(room, actorId, action);
        if (action.type.equals("start")) {
            room.runtime = new GameRuntime();
            room.runtime.ruleSetVersion = rules.active().getVersion(); room.runtime.ruleSetDigest = rules.active().getDigest();
            scope.run(room, engine, null, null, () -> process.getRuntimeService().startProcessInstanceByKey(PROCESS_KEY, room.gameId, Map.of("roomCode", room.code, "gameId", room.gameId, "ruleSetVersion", room.runtime.ruleSetVersion)));
        }
        if (action.type.equals("newGame")) room.runtime = null;
        refresh(room, engine);
    }
    /** 输入候选房间，处理在线状态、服刑和交易超时，变化后同步通用等待任务。 */
    public void refresh(Room room, GameEngine engine) {
        requireTransaction(); engine.turns.refresh(room, System.currentTimeMillis());
        if (room.runtime != null && (!Objects.equals(room.runtime.decisionKey, GameWorkflowDelegates.decisionKey(room)) || engine.needsAdvance(room))) complete(room, engine, null, null);
    }
    /** 输入候选和可选玩家操作，完成已有任务；交易取消仍由领域规则校验发起者。 */
    private void complete(Room room, GameEngine engine, String actorId, Action action) {
        if (room.runtime.taskId == null) throw new GameError("当前没有可继续的流程任务");
        try { scope.run(room, engine, actorId, action, () -> process.getTaskService().complete(room.runtime.taskId)); }
        catch (FlowableException error) {
            // 引擎可能包装 Delegate 的业务拒绝，保留原前端错误码和提示。
            for (Throwable cause = error.getCause(); cause != null; cause = cause.getCause()) if (cause instanceof GameError game) throw game;
            throw error;
        }
    }
    /** 缺失外层事务时拒绝推进，防止流程与房间快照分开提交。 */
    private void requireTransaction() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) throw new IllegalStateException("游戏流程必须与房间快照共用事务");
    }
}
