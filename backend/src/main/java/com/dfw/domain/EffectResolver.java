package com.dfw.domain;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.function.BiConsumer;

/** 同步执行原子效果，所有对局按持久化队列展开。 */
public class EffectResolver {
    private final RandomSource random;
    private final Map<String, BiConsumer<Room, GameEffect>> handlers = new HashMap<>();
    private final ThreadLocal<Integer> insertionIndex = new ThreadLocal<>();
    /** 输入随机源，用于分配效果标识。 */
    public EffectResolver(RandomSource random) { this.random = random; }
    /** 输入类型及处理器，启动组装时注册一次，重复注册拒绝。 */
    public void register(String type, BiConsumer<Room, GameEffect> handler) {
        if (handlers.putIfAbsent(type, handler) != null) throw new IllegalStateException("重复效果处理器：" + type);
    }
    /** 输入类型和作用玩家，返回尚未入队的效果参数。 */
    public GameEffect effect(String type, Player player) {
        GameEffect effect = new GameEffect(); effect.id = random.id(); effect.type = type; effect.playerId = player.id; return effect;
    }
    /** 输入房间和效果，添加到当前效果之后、其兄弟效果之前，保证联动先完成。 */
    public void submit(Room room, GameEffect effect) {
        Integer index = insertionIndex.get();
        if (index == null && (room.runtime.effectsWaiting || room.activity != null) && effect.resumesDecision) room.runtime.effects.addFirst(effect);
        else if (index == null) room.runtime.effects.add(effect);
        else { room.runtime.effects.add(index, effect); insertionIndex.set(index + 1); }
    }
    /** 输入有序组合，把解决当前选择的整组效果插入暂停组合之前，保持组内顺序。 */
    public void submitAll(Room room, List<GameEffect> effects) {
        if (insertionIndex.get() == null && (room.runtime.effectsWaiting || room.activity != null) && !effects.isEmpty() && effects.getFirst().resumesDecision) room.runtime.effects.addAll(0, effects);
        else effects.forEach(effect -> submit(room, effect));
    }
    /** 输入房间，结算队列至选择或为空；解决选择的效果先于暂停的后续组合。 */
    public void drain(Room room) {
        if (!room.status.equals("playing")) { room.runtime.effects.clear(); room.runtime.effectsWaiting = false; return; }
        if (room.pending == null) room.runtime.effectsWaiting = false;
        int count = 0;
        while (!room.runtime.effects.isEmpty()) {
            if (room.runtime.effectsWaiting && room.pending != null && !room.runtime.effects.getFirst().resumesDecision) return;
            if (++count > 256) throw new GameError("效果联动超过本次结算上限");
            int turn = room.turnNumber;
            GameEffect effect = room.runtime.effects.getFirst();
            if (room.activity != null && !effect.resumesDecision) return;
            insertionIndex.set(1);
            try { handler(effect).accept(room, effect); }
            finally { insertionIndex.remove(); }
            SettlementService.observe(room);
            HistoryTrace.event(room, "effect", effect.playerId, "游戏效果已结算", effect);
            room.runtime.effects.removeFirst(); room.runtime.settledEffects++;
            // 全体扣款在批次完成前不换人；真正换回合后取消原行动者的剩余步骤。
            if (!room.status.equals("playing") || room.turnNumber != turn) { room.runtime.effects.clear(); room.runtime.effectsWaiting = false; return; }
            room.runtime.effectsWaiting = room.pending != null;
            if (room.runtime.effectsWaiting) return;
        }
    }
    /** 输入效果参数，返回固定注册的处理器，未知类型拒绝。 */
    private BiConsumer<Room, GameEffect> handler(GameEffect effect) {
        BiConsumer<Room, GameEffect> handler = handlers.get(effect.type);
        if (handler == null) throw new GameError("未知效果：" + effect.type);
        return handler;
    }
}
