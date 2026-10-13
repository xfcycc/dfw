package com.dfw.domain;

import java.util.*;
import com.dfw.domain.rules.RuleSet;
import com.dfw.domain.rules.RuleEffects;
import com.dfw.domain.rules.RuleExpression;
import static com.dfw.domain.GameSupport.*;

/** 回合、移动、落点和在线暂停规则。 */
public class TurnService {
    private final EconomyService economy;
    private final RandomSource random;
    private final EffectResolver effects;
    private final RuleSet rules;
    /** 输入经济规则与独立随机源，构造回合服务。 */
    public TurnService(EconomyService economy, RandomSource random, EffectResolver effects, RuleSet rules) {
        this.economy = economy; this.random = random; this.effects = effects; this.rules = rules;
        effects.register("DRAW_CARDS", this::drawCards);
        effects.register("SKIP_TURN", this::skipTurn);
        effects.register("MOVE", (room, effect) -> { move(room, player(room, effect.playerId), (int) effect.amount); if (effect.message != null) log(room, effect.message); });
        effects.register("SETTLE_TILE", (room, effect) -> settle(room, player(room, effect.playerId)));
        effects.register("PAY_FEE", (room, effect) -> economy.charge(room, player(room, effect.playerId), effect.amount, effect.targetId, effect.tileId));
        effects.register("SETTLE_PAYMENT", (room, effect) -> economy.pay(room, player(room, effect.playerId)));
        effects.register("DECLARE_BANKRUPTCY", (room, effect) -> {
            Player payer = player(room, effect.playerId); economy.declareBankruptcy(room, payer);
            if (room.runtime.globalEvent == null) { if (current(room) == payer) advance(room); else checkWinner(room); }
        });
    }
    /** 输入玩家和数量，增加私人道具，不记录种类与数量。 */
    public void giveCards(Player player, int count) {
        for (int i = 0; i < count; i++) { Card card = new Card(); card.id = random.id(); card.kind = Rules.ITEMS[random.nextInt(Rules.ITEMS.length)]; player.hand.add(card); }
    }
    /** 输入私人补给效果，增加作用玩家手牌，公开日志不包含种类与数量。 */
    private void drawCards(Room room, GameEffect effect) {
        Player owner = player(room, effect.playerId); if (owner == null || owner.bankrupt) return;
        giveCards(owner, Math.toIntExact(effect.amount)); log(room, owner.name + " 收到一份私人补给");
    }
    /** 输入休息效果，设置后续跳过次数，保留普通休息与监禁的既有区别。 */
    private void skipTurn(Room room, GameEffect effect) {
        Player owner = player(room, effect.playerId); if (owner == null || owner.bankrupt) return;
        owner.skipTurns = Math.toIntExact(effect.amount); log(room, owner.name + " 下次行动休息");
    }
    /** 输入房间，仅余一人时结束游戏并返回真。 */
    public boolean checkWinner(Room room) {
        List<Player> alive = room.players.stream().filter(p -> !p.bankrupt).toList();
        if (alive.size() > 1) return false;
        room.status = "finished"; room.winnerId = alive.isEmpty() ? null : alive.getFirst().id;
        room.pending = null; room.trade = null; room.pausedReason = null;
        log(room, alive.isEmpty() ? "本局结束" : alive.getFirst().name + " 成为最后的赢家！"); return true;
    }
    /** 输入房间，切换下一行动者并重置回合标志。 */
    public void advance(Room room) {
        StatusService.endTurn(room, current(room));
        if (checkWinner(room)) return;
        do { room.turnIndex = (room.turnIndex + 1) % room.turnOrder.size(); } while (current(room).bankrupt);
        room.turnNumber++; room.phase = "pre_roll"; room.pending = null; room.dice = null;
        room.activeCardUsed = false; room.eventDrawn = false; room.upgradedTiles.clear();
    }
    /** 输入费用参数，打开可管理资产和协商抵债的支付选择。 */
    public void charge(Room room, Player player, long amount, String creditorId, Integer tileId) {
        GameEffect effect = effects.effect("PAY_FEE", player); effect.amount = amount; effect.targetId = creditorId; effect.tileId = tileId;
        effects.submit(room, effect);
    }
    /** 输入当前付款人，将确认支付插在父流程暂停的效果之前。 */
    public void pay(Room room, Player player) {
        GameEffect effect = effects.effect("SETTLE_PAYMENT", player); effect.resumesDecision = true; effects.submit(room, effect);
    }
    /** 输入当前债务人，将主动清算插入费用等待点，全图批次保持原游标。 */
    public void declareBankruptcy(Room room, Player player) {
        GameEffect effect = effects.effect("DECLARE_BANKRUPTCY", player); effect.resumesDecision = true; effects.submit(room, effect);
    }
    /** 输入玩家和有符号步数，逐格处理路障和正向起点收入。 */
    public void move(Room room, Player player, int steps) {
        Motion motion = new Motion(); motion.id = random.id(); motion.playerId = player.id; motion.from = player.position;
        for (int i = 0; i < Math.abs(steps); i++) {
            player.position = Math.floorMod(player.position + Integer.signum(steps), room.board.size()); motion.path.add(player.position);
            if (steps > 0 && player.position == 0) { long income = Rules.economy(room).money("startIncome", Rules.START_INCOME); player.cash += income; log(room, player.name + " 经过起点，获得 " + income); }
            Tile tile = room.board.get(player.position);
            if (tile.barrier && StatusService.immune(player, "TRAP")) { log(room, player.name + " 免疫 " + tile.name + " 的路障，路障保留"); continue; }
            if (tile.barrier) { tile.barrier = false; log(room, player.name + " 在 " + tile.name + " 被路障拦下"); break; }
        }
        room.motion = motion;
    }
    /** 输入落点玩家，处理地产或特殊格，每回合最多抽一次事件。 */
    public void settle(Room room, Player player) {
        Tile tile = room.board.get(player.position); room.phase = "manage"; room.pending = null;
        switch (tile.type) {
            case "jail" -> { player.skipTurns = 1; player.jailed = true; log(room, player.name + " 进入监狱，下次行动休息"); }
            case "chance", "fortune" -> {
                if (room.eventDrawn) return; room.eventDrawn = true;
                var pool = tile.type.equals("chance") ? rules.getChanceEvents() : rules.getFortuneEvents();
                var event = pool.get(random.nextInt(pool.size()));
                effects.submitAll(room, RuleEffects.plan(event.steps(), new RuleExpression.Context(room, player, new Action()), effects, event.id()));
            }
            case "property" -> {
                if (tile.ownerId == null) { room.phase = "buy"; room.pending = pending("buy", player, tile.id, tile.price, null); }
                else if (tile.ownerId.equals(player.id)) {
                    if (!tile.mortgaged && tile.level < Rules.MAX_LEVEL && !room.upgradedTiles.contains(tile.id)) { room.phase = "upgrade"; room.pending = pending("upgrade", player, tile.id, Rules.buildCost(room, tile), null); }
                } else if (!tile.mortgaged) {
                    charge(room, player, Rules.rent(room, tile), tile.ownerId, tile.id);
                }
            }
            default -> { }
        }
    }
    /** 输入房间，更新房主和断线暂停，清除旧报价计时数据但保留待处理报价。 */
    public void refresh(Room room) {
        Player host = player(room, room.hostId);
        if (host == null || !host.online) room.players.stream().filter(p -> p.online).findFirst().ifPresent(p -> room.hostId = p.id);
        if (!room.status.equals("playing")) { room.pausedReason = null; return; }
        Player actor = room.pending != null ? player(room, room.pending.playerId) : room.runtime != null && room.runtime.globalEvent != null ? null : current(room);
        Player offline = room.players.stream().filter(p -> !p.online && ((actor != null && p.id.equals(actor.id)) || (room.trade != null && p.id.equals(room.trade.toId)))).findFirst().orElse(null);
        room.pausedReason = offline == null ? null : "等待 " + offline.name + " 重新连接";
        if (room.trade != null) {
            room.trade.remainingMs = 0; room.trade.runningSince = null;
        }
    }
    /** 输入配置对局，在 TURN_START 按原规则跳过在线玩家的一次休息回合。 */
    public boolean skipCurrentTurn(Room room) {
        Player actor = current(room);
        if (actor == null || !actor.online || actor.skipTurns == 0 || room.trade != null) return false;
        actor.skipTurns--; actor.jailed = false; log(room, actor.name + " 休息一回合"); advance(room); return true;
    }
}
