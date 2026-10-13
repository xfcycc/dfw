package com.dfw.domain;

import java.util.List;
import static com.dfw.domain.GameSupport.*;
import static com.dfw.domain.GameError.require;

/** 全图事件的有序结算与实际建筑损失补偿，不自行写库或推进流程。 */
public final class EventService {
    private final EffectResolver effects;
    private final EconomyService economy;
    private final TurnService turns;
    private final RandomSource random;
    /** 输入既有经济、回合和随机服务，注册首期事件原语。 */
    public EventService(EffectResolver effects, EconomyService economy, TurnService turns, RandomSource random) {
        this.effects = effects; this.economy = economy; this.turns = turns; this.random = random;
        effects.register("CHARGE_ALL", this::chargeAll);
        effects.register("CHARGE_GLOBAL_TARGET", this::chargeTarget);
        effects.register("FINISH_GLOBAL_EVENT", this::finish);
        effects.register("ADD_STATUS", StatusService::add);
        effects.register("DESTROY_BUILDINGS", this::destroy);
    }
    /** 输入全体费用效果，固定座位目标并展开子效果，原 pending 之外保存返回位置。 */
    private void chargeAll(Room room, GameEffect effect) {
        require(effect.amount >= 0 && room.runtime.globalEvent == null && room.pending == null, "全体费用结算上下文不可用");
        GlobalEventFrame frame = new GlobalEventFrame(); frame.effectId = effect.id; frame.amount = effect.amount; frame.returnPhase = room.phase;
        frame.targets.addAll(room.players.stream().filter(value -> !value.bankrupt).map(value -> value.id).toList());
        room.runtime.globalEvent = frame;
        log(room, "全图事件：所有未破产玩家各向银行支付 " + effect.amount);
        for (String id : frame.targets) effects.submit(room, effects.effect("CHARGE_GLOBAL_TARGET", player(room, id)));
        effects.submit(room, effects.effect("FINISH_GLOBAL_EVENT", player(room, effect.playerId)));
    }
    /** 输入单个固定目标，复用偿债或清算；先保存下一目标，等待费用由 pending 恢复。 */
    private void chargeTarget(Room room, GameEffect effect) {
        GlobalEventFrame frame = room.runtime.globalEvent;
        require(frame != null && frame.nextTarget < frame.targets.size() && frame.targets.get(frame.nextTarget).equals(effect.playerId), "全图事件目标游标不一致");
        frame.nextTarget++;
        Player target = player(room, effect.playerId);
        if (target != null && !target.bankrupt) economy.charge(room, target, frame.amount, null, null);
    }
    /** 输入批次结束效果，完成全部目标后统一检查胜负，必要时切换原行动者。 */
    private void finish(Room room, GameEffect effect) {
        GlobalEventFrame frame = room.runtime.globalEvent;
        require(frame != null && frame.nextTarget == frame.targets.size() && room.pending == null, "全图事件尚未完成");
        room.phase = frame.returnPhase; room.runtime.globalEvent = null;
        if (!turns.checkWinner(room) && current(room).bankrupt) turns.advance(room);
    }
    /** 输入指定地块或随机有建筑地块，记录实际损失后按业主判定一次补偿，空目标无赔付。 */
    private void destroy(Room room, GameEffect effect) {
        require(effect.probability >= 0 && effect.probability <= 100 && effect.compensationPercent >= 0 && effect.compensationPercent <= 100, "补偿参数不可用");
        Tile target = tile(room, effect.tileId);
        if ("RANDOM_BUILT_PROPERTY".equals(effect.selector)) {
            List<Tile> candidates = room.board.stream().filter(value -> value.type.equals("property") && value.ownerId != null && value.level > 0).toList();
            if (candidates.isEmpty()) { log(room, "龙卷风经过，当前没有可摧毁的建筑"); return; }
            target = candidates.get(random.nextInt(candidates.size()));
        }
        require(target != null && target.type.equals("property"), "请选择有效地产");
        Player owner = player(room, target.ownerId);
        if (target.level == 0 || owner == null || owner.bankrupt) { log(room, target.name + " 没有实际建筑损失，不触发补偿"); return; }
        DisasterResult result = new DisasterResult(); result.effectId = effect.id; result.tileId = target.id; result.ownerId = owner.id;
        result.destroyedLevels = target.level; result.loss = Math.multiplyExact(Rules.buildCost(target), target.level);
        target.level = 0; result.sample = random.nextInt(100);
        if (result.sample < effect.probability) { result.compensation = Math.multiplyExact(result.loss, effect.compensationPercent) / 100; owner.cash = Math.addExact(owner.cash, result.compensation); }
        room.runtime.lastDisaster = result;
        log(room, target.name + " 的 " + result.destroyedLevels + " 级建筑被摧毁，" + owner.name + " 损失 " + result.loss + "，获得补偿 " + result.compensation);
    }
}
