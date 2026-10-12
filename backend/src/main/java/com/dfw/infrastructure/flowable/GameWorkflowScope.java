package com.dfw.infrastructure.flowable;

import com.dfw.domain.Action;
import com.dfw.domain.GameEngine;
import com.dfw.domain.Room;
import org.springframework.stereotype.Component;

/** 同步流程调用的候选聚合作用域，不将房间或手牌放入流程变量。 */
@Component
public class GameWorkflowScope {
    private final ThreadLocal<Context> current = new ThreadLocal<>();
    /** 输入候选房间和命令，在本线程执行同步流程回调，结束后清理作用域。 */
    public void run(Room room, GameEngine engine, String actorId, Action action, Runnable operation) {
        if (current.get() != null) throw new IllegalStateException("不允许嵌套游戏流程作用域");
        current.set(new Context(room, engine, actorId, action));
        try { operation.run(); } finally { current.remove(); }
    }
    /** 返回本次同步流程上下文，后台异步执行时拒绝。 */
    public Context context() {
        Context context = current.get();
        if (context == null) throw new IllegalStateException("游戏流程必须在房间命令事务内同步执行");
        return context;
    }
    /** 仅在同步调用期间存在的聚合、规则和输入。 */
    public record Context(Room room, GameEngine engine, String actorId, Action action) {}
}
