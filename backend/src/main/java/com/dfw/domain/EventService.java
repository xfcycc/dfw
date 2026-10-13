package com.dfw.domain;

import java.util.List;
import com.dfw.domain.rules.RuleSet;
import static com.dfw.domain.GameSupport.*;
import static com.dfw.domain.GameError.require;

/** 全图事件、实际建筑损失与补偿订阅，不自行写库或推进流程。 */
public final class EventService {
    private final EffectResolver effects;
    private final EconomyService economy;
    private final TurnService turns;
    private final RandomSource random;
    private final RuleSet rules;
    /** 输入经济、回合、随机服务及当前规则集，注册事件、补偿判定和银行奖励原语。 */
    public EventService(EffectResolver effects, EconomyService economy, TurnService turns, RandomSource random, RuleSet rules) {
        this.effects = effects; this.economy = economy; this.turns = turns; this.random = random; this.rules = rules;
        effects.register("CHARGE_ALL", this::chargeAll);
        effects.register("CHARGE_GLOBAL_TARGET", this::chargeTarget);
        effects.register("FINISH_GLOBAL_EVENT", this::finish);
        effects.register("ADD_STATUS", StatusService::add);
        effects.register("DESTROY_BUILDINGS", this::destroy);
        effects.register("RANDOM_COMPENSATION", this::compensate);
        effects.register("GRANT_MONEY", this::grant);
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
    /** 输入指定或随机地块，保存实际损失后触发当前配置订阅，空目标无事实。 */
    private void destroy(Room room, GameEffect effect) {
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
        result.destroyedLevels = target.level; result.loss = Math.multiplyExact(Rules.buildCost(room, target), target.level);
        target.level = 0; result.eventId = random.id(); room.runtime.lastDisaster = result;
        log(room, target.name + " 的 " + result.destroyedLevels + " 级建筑被摧毁，" + owner.name + " 损失 " + result.loss);
        publishDestroyed(room, result, owner);
    }
    /** 输入实际损失快照，按当前规则顺序保存匹配明细并插入子效果；不接受客户端伪造事实。 */
    private void publishDestroyed(Room room, DisasterResult result, Player owner) {
        for (var rule : rules.getPassiveRules()) {
            if (!rule.trigger().equals(result.type) || !rule.tag().equals(result.tag) || result.loss <= 0) continue;
            CompensationResult matched = new CompensationResult(); matched.ruleId = rule.id(); result.compensations.add(matched);
            GameEffect check = effects.effect("RANDOM_COMPENSATION", owner); check.sourceEventId = result.eventId; check.passiveRuleId = rule.id();
            check.amount = result.loss; check.probability = rule.probability(); check.compensationPercent = rule.compensationPercent();
            effects.submit(room, check);
        }
    }
    /** 输入已匹配订阅，按损失快照抽样一次，保存判定并将奖励插在后续兄弟效果之前。 */
    private void compensate(Room room, GameEffect effect) {
        DisasterResult event = sourceEvent(room, effect); CompensationResult result = compensation(event, effect.passiveRuleId);
        if (result.sample != null) return;
        Player owner = player(room, effect.playerId);
        if (owner == null || owner.bankrupt) return;
        require(event.ownerId.equals(owner.id) && event.loss == effect.amount, "补偿损失快照不一致");
        result.sample = random.nextInt(100);
        if (event.compensations.getFirst() == result) event.sample = result.sample;
        if (result.sample >= effect.probability) { log(room, owner.name + " 本次灾害未获得补偿"); return; }
        result.amount = Math.multiplyExact(effect.amount, effect.compensationPercent) / 100;
        GameEffect grant = effects.effect("GRANT_MONEY", owner); grant.amount = result.amount;
        grant.sourceEventId = event.eventId; grant.passiveRuleId = effect.passiveRuleId; effects.submit(room, grant);
    }
    /** 输入银行奖励及可选损失来源，复核收款人，支付并记录实际补偿；不经过可暂停的扣款路径。 */
    private void grant(Room room, GameEffect effect) {
        require(effect.amount >= 0, "奖励金额不可用");
        DisasterResult event = effect.sourceEventId == null ? null : sourceEvent(room, effect);
        CompensationResult result = event == null ? null : compensation(event, effect.passiveRuleId);
        if (result != null) { if (result.settled) return; require(result.sample != null && result.amount == effect.amount && event.ownerId.equals(effect.playerId), "补偿支付进度不一致"); result.settled = true; }
        Player owner = player(room, effect.playerId);
        if (owner == null || owner.bankrupt) return;
        owner.cash = Math.addExact(owner.cash, effect.amount);
        if (result != null) { result.paid = effect.amount; event.compensation = Math.addExact(event.compensation, effect.amount); }
        log(room, effect.message != null ? effect.message : owner.name + (event == null ? " 获得银行奖励 " : " 获得灾害补偿 ") + effect.amount);
    }
    /** 输入事实关联的子效果，返回已保存的实际损失；错配时报告诊断并回滚当前自动段。 */
    private static DisasterResult sourceEvent(Room room, GameEffect effect) {
        DisasterResult event = room.runtime.lastDisaster;
        require(event != null && event.eventId != null && event.eventId.equals(effect.sourceEventId), "补偿来源事件不一致"); return event;
    }
    /** 输入实际损失和订阅标识，返回已固定的匹配进度，不在恢复时重新匹配配置。 */
    private static CompensationResult compensation(DisasterResult event, String ruleId) {
        return event.compensations.stream().filter(result -> result.ruleId.equals(ruleId)).findFirst().orElseThrow(() -> new GameError("补偿订阅进度缺失"));
    }
}
