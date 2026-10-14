package com.dfw.domain;

import java.util.*;
import java.util.function.Supplier;

/** 一次提交内的事实收集器；事务失败时丢弃，不在领域循环中访问数据库。 */
public final class HistoryTrace {
    /** 保存当时上下文和内部明细，公开接口只返回白名单文字。 */
    public record Entry(String gameId, Integer turnNumber, String phase, String actorId, String actorName,
            String type, String text, long at, Object details) { }
    private static final ThreadLocal<List<Entry>> CURRENT = new ThreadLocal<>();
    private HistoryTrace() { }
    /** 输入提交回调，收集其事实并在退出时清理线程上下文，返回回调结果。 */
    public static <T> T capture(Supplier<T> operation) {
        if (CURRENT.get() != null) return operation.get();
        CURRENT.set(new ArrayList<>());
        try { return operation.get(); } finally { CURRENT.remove(); }
    }
    /** 输入房间、事实类型、作用者和明细，在实际发生时记录顺序与上下文。 */
    public static void event(Room room, String type, String actorId, String text, Object details) {
        List<Entry> entries = CURRENT.get(); if (entries == null) return;
        Player actor = actorId == null ? null : GameSupport.player(room, actorId);
        entries.add(new Entry(room.gameId, room.turnNumber, room.phase, actorId, actor == null ? null : actor.name,
                type, text, System.currentTimeMillis(), details));
    }
    /** 返回本次提交的已收集事实，调用者批量持久化；不暴露可变缓冲区。 */
    public static List<Entry> entries() { return CURRENT.get() == null ? List.of() : List.copyOf(CURRENT.get()); }
}
