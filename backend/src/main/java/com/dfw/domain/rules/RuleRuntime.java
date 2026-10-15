package com.dfw.domain.rules;

import com.dfw.domain.*;
import java.util.*;
import static com.dfw.domain.GameError.require;
import static com.dfw.domain.GameSupport.*;

/** 持久化阶段游标和最小道具 DSL 执行，不依赖 Flowable 或网络协议实现。 */
public final class RuleRuntime {
    private final RuleSet catalog;
    private final EffectResolver effects;
    private final TurnService turns;
    private final RandomSource random;
    private final ContentService content;
    private final ActivityService activities;
    /** 输入当前规则集、基础结算及随机源，注册替代骰子和公开消息原语。 */
    public RuleRuntime(RuleSet catalog, EffectResolver effects, TurnService turns, RandomSource random, ContentService content, ActivityService activities) {
        this.catalog = catalog; this.effects = effects; this.turns = turns; this.random = random; this.content = content; this.activities = activities;
        effects.register("REPLACE_ROLL", (room, effect) -> {
            require(effect.amount >= -3 && effect.amount <= 6, "规则骰子点数不可用");
            room.runtime.rollMode = "REPLACE"; room.runtime.movementSteps = (int) effect.amount; room.dice = (int) effect.amount;
            if (stage(room).handler().equals("ACTION_WINDOW")) room.runtime.windowExit = "replace_roll";
            if (effect.message != null) log(room, effect.message);
        });
        effects.register("LOG", (room, effect) -> log(room, effect.message));
    }
    /** 输入房间和种类，返回当前配置是否提供租金防御发动方式。 */
    public boolean rentDefense(Room room, String kind) {
        var rule = catalog.getCards().get(kind); return rule != null && rule.activation().equals("RENT");
    }
    /** 输入道具命令，按配置检查窗口、次数和目标，完整解析组合后才消耗手牌。 */
    public void useCard(Room room, Player actor, Action action) {
        Card card = actor.hand.stream().filter(value -> value.id.equals(action.cardId)).findFirst().orElse(null);
        var rule = card == null ? null : catalog.getCards().get(card.kind);
        require(rule != null && rule.activation().equals("ACTIVE"), "该道具不可主动使用");
        require(rule.stages().contains(stage(room).id()) && (!rule.countsAsActive() || !room.activeCardUsed), "每回合掷骰前最多主动使用一张道具");
        executeCard(room, actor, action, card, rule);
    }
    /** 输入收费玩家，以当前租金选择上下文执行配置中的防御卡，不占主动次数。 */
    public void immune(Room room, Player actor, String cardId) {
        Card card = actor.hand.stream().filter(value -> value.id.equals(cardId) && rentDefense(room, value.kind)).findFirst().orElse(null);
        require(card != null, "你没有可用的防御道具"); var rule = catalog.getCards().get(card.kind);
        require(rule.stages().contains("rent_choice") && room.phase.equals("rent"), "当前没有待支付的租金");
        executeCard(room, actor, new Action(), card, rule);
    }
    /** 输入道具与发动参数，先校验并解析全部效果，再原子消费并入队。 */
    private void executeCard(Room room, Player actor, Action action, Card card, RuleSet.CardRule rule) {
        var context = new RuleExpression.Context(room, actor, action);
        for (var check : rule.checks()) require(check.condition().matches(context), check.error());
        List<GameEffect> planned = RuleEffects.plan(rule.steps(), context, effects, rule.id());
        if (rule.activation().equals("RENT")) planned.getFirst().resumesDecision = true;
        actor.hand.remove(card); if (rule.countsAsActive()) room.activeCardUsed = true; cardLog(room, actor, card, action);
        effects.submitAll(room, planned);
    }
    /** 输入原协议命令，检查当前配置行动窗口；买地、租金和偿债继续原待选校验。 */
    public void validateAction(Room room, String action) {
        if (room.pending != null || !Set.of("roll", "card", "tradeOffer", "sellBuilding", "mortgage", "redeem", "sellLand", "endTurn").contains(action)) return;
        var stage = stage(room);
        require(stage.handler().equals("ACTION_WINDOW") && stage.actions().contains(action), "当前操作不可用");
    }
    /** 输入成功命令，保存其窗口退出结果；交易等待期间保留原窗口返回位置。 */
    public void accepted(Room room, String action) {
        var stage = stage(room);
        if (stage.handler().equals("ACTION_WINDOW") && stage.exitOn().contains(action)) room.runtime.windowExit = action;
    }
    /** 输入房间，返回连接或系统变更后是否需通过通用流程继续自动阶段。 */
    public boolean needsAdvance(Room room) {
        if (!room.status.equals("playing") || room.pausedReason != null || room.trade != null) return false;
        if (room.activity != null) return activities.due(room);
        if (content.needsOverflow(room)) return true;
        if (room.pending != null) return false;
        return !room.runtime.effects.isEmpty() || room.runtime.stageTurnNumber != room.turnNumber || !stage(room).handler().equals("ACTION_WINDOW") || room.runtime.windowExit != null;
    }
    /** 输入候选房间，逐阶段结算至行动窗口、落点选择、交易等待或终局。 */
    public void advanceUntilWait(Room room) {
        for (int work = 0; work < 256; work++) {
            if (room.status.equals("playing")) alignTurn(room);
            if (room.activity == null) content.overflow(room);
            effects.drain(room);
            boolean hadActivity = room.activity != null; activities.refresh(room);
            if (hadActivity && room.activity == null) { if (turns.checkWinner(room)) return; if (current(room).bankrupt) turns.advance(room); continue; }
            turns.refresh(room);
            if (room.activity != null) return;
            if (!room.status.equals("playing") || room.pending != null || room.trade != null || room.pausedReason != null) return;
            alignTurn(room); var stage = stage(room);
            switch (stage.handler()) {
                case "AUTO_HOOK" -> {
                    if (stage.hook().equals("TURN_START") && turns.skipCurrentTurn(room)) continue;
                    var context = new RuleExpression.Context(room, current(room), new Action());
                    List<GameEffect> planned = RuleEffects.plan(stage.steps(), context, effects, stage.id()); room.runtime.stageIndex++;
                    effects.submitAll(room, planned);
                }
                case "ACTION_WINDOW" -> {
                    room.phase = stage.phase();
                    if (room.runtime.windowExit == null) return;
                    require(stage.exitOn().contains(room.runtime.windowExit), "行动窗口退出结果不可用");
                    room.runtime.windowExit = null; room.runtime.stageIndex++;
                }
                case "ROLL" -> {
                    require(room.runtime.rollMode != null, "缺少掷骰行动结果");
                    if (room.runtime.rollMode.equals("RANDOM")) { room.dice = random.nextInt(6) + 1; room.runtime.movementSteps = room.dice + room.runtime.rollBonus; log(room, current(room).name + " 掷出 " + room.dice + " 点" + (room.runtime.rollBonus == 0 ? "" : "，加码后移动 " + room.runtime.movementSteps + " 格")); room.runtime.rollBonus = 0; }
                    HistoryTrace.event(room, "dice", current(room).id, "本次移动点数为 " + room.dice, room.runtime.movementSteps);
                    room.runtime.stageIndex++;
                }
                case "MOVEMENT" -> {
                    require(room.runtime.movementSteps != null, "缺少移动步数");
                    GameEffect movement = effects.effect("MOVE", current(room)); movement.amount = room.runtime.movementSteps;
                    room.runtime.stageIndex++; effects.submit(room, movement);
                }
                case "LANDING" -> { room.runtime.stageIndex++; effects.submit(room, effects.effect("SETTLE_TILE", current(room))); }
                case "END_TURN" -> turns.advance(room);
                default -> throw new IllegalStateException("未注册阶段：" + stage.handler());
            }
        }
        throw new GameError("回合阶段推进超过本次结算上限");
    }
    /** 输入房间，在进入新回合时重置阶段位置，不重复已提交阶段。 */
    private void alignTurn(Room room) {
        if (room.runtime.stageTurnNumber == room.turnNumber) return;
        if (room.runtime.stageTurnNumber != 0) { room.runtime.effects.clear(); room.runtime.effectsWaiting = false; }
        room.runtime.stageTurnNumber = room.turnNumber; room.runtime.stageIndex = 0;
        room.runtime.windowExit = null; room.runtime.rollMode = null; room.runtime.movementSteps = null; room.runtime.rollBonus = 0;
    }
    /** 输入房间，返回当前配置中的阶段，错误游标拒绝继续。 */
    private RuleSet.Stage stage(Room room) {
        List<RuleSet.Stage> stages = catalog.getStages();
        if (room.runtime.stageIndex < 0 || room.runtime.stageIndex >= stages.size()) throw new IllegalStateException("房间阶段游标不可用：" + room.code);
        return stages.get(room.runtime.stageIndex);
    }
}
