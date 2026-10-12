package com.dfw.domain;

import static com.dfw.domain.GameError.require;
import static com.dfw.domain.GameSupport.*;
import com.dfw.domain.rules.RuleCatalog;
import com.dfw.domain.rules.RuleRuntime;

/** 六种道具的验证和消耗。 */
public class ItemService {
    private final TurnService turns;
    private final RandomSource random;
    private final EffectResolver effects;
    private final RuleCatalog rules;
    private final RuleRuntime ruleRuntime;
    /** 输入回合服务与随机源，构造道具服务。 */
    public ItemService(TurnService turns, RandomSource random, EffectResolver effects, RuleCatalog rules, RuleRuntime ruleRuntime) {
        this.turns = turns; this.random = random; this.effects = effects; this.rules = rules; this.ruleRuntime = ruleRuntime;
        effects.register("PLACE_BARRIER", this::resolve);
        effects.register("DEMOLISH", this::resolve);
        effects.register("SWAP_POSITION", this::resolve);
        effects.register("WAIVE_RENT", this::resolve);
    }
    /** 输入房间、玩家和道具操作，验证后消耗并执行效果。 */
    public void use(Room room, Player player, Action action) {
        if (rules.configured(room)) { ruleRuntime.useCard(room, player, action); return; }
        require(room.phase.equals("pre_roll") && !room.activeCardUsed, "每回合掷骰前最多主动使用一张道具");
        Card card = player.hand.stream().filter(c -> c.id.equals(action.cardId)).findFirst().orElse(null);
        require(card != null && !card.kind.equals("immunity"), "该道具不可主动使用");
        Tile tile = tile(room, action.tileId); Player target = player(room, action.playerId);
        switch (card.kind) {
            case "remote" -> require(action.point != null && action.point >= 1 && action.point <= 6, "请选择 1～6 点");
            case "barrier" -> { int distance = tile == null ? 0 : Math.floorMod(tile.id - player.position, room.board.size()); require(tile != null && tile.type.equals("property") && !tile.barrier && distance >= 1 && distance <= 6, "请选择前方六格内没有路障的地产"); }
            case "demolish" -> require(tile != null && tile.ownerId != null && !tile.ownerId.equals(player.id) && tile.level > 0, "请选择对手有建筑的地产");
            case "swap" -> require(target != null && !target.id.equals(player.id) && !target.bankrupt && !target.jailed, "请选择未破产、未服刑的对手");
            default -> { }
        }
        player.hand.remove(card); room.activeCardUsed = true;
        if (room.runtime != null) {
            switch (card.kind) {
                case "remote", "stay" -> {
                    int steps = card.kind.equals("remote") ? action.point : 0; room.dice = steps;
                    log(room, card.kind.equals("remote") ? player.name + " 本次移动 " + steps + " 格" : player.name + " 留在当前位置");
                    turns.moveAndSettle(room, player, steps, null);
                }
                case "barrier", "demolish", "swap" -> {
                    String type = switch (card.kind) { case "barrier" -> "PLACE_BARRIER"; case "demolish" -> "DEMOLISH"; default -> "SWAP_POSITION"; };
                    GameEffect effect = effects.effect(type, player); effect.tileId = action.tileId; effect.targetId = action.playerId; effects.submit(room, effect);
                }
                default -> throw new GameError("该道具不可主动使用");
            }
            return;
        }
        switch (card.kind) {
            case "remote" -> { room.dice = action.point; log(room, player.name + " 本次移动 " + action.point + " 格"); turns.move(room, player, action.point); turns.settle(room, player); }
            case "stay" -> { room.dice = 0; Motion motion = new Motion(); motion.id = random.id(); motion.playerId = player.id; motion.from = player.position; room.motion = motion; log(room, player.name + " 留在当前位置"); turns.settle(room, player); }
            case "barrier" -> { tile.barrier = true; log(room, player.name + " 在 " + tile.name + " 设置了路障"); }
            case "demolish" -> { tile.level--; log(room, tile.name + " 的建筑降低一级"); }
            case "swap" -> { int position = player.position; player.position = target.position; target.position = position; log(room, player.name + " 与 " + target.name + " 交换了位置"); }
            default -> throw new GameError("该道具不可主动使用");
        }
    }
    /** 输入收费玩家，消耗一张免租卡并清除本次租金。 */
    public void immune(Room room, Player player) {
        if (rules.configured(room)) { ruleRuntime.immune(room, player); return; }
        Card card = player.hand.stream().filter(c -> c.kind.equals("immunity")).findFirst().orElse(null);
        require(card != null, "你没有可用的防御道具"); player.hand.remove(card);
        if (room.runtime != null) { GameEffect effect = effects.effect("WAIVE_RENT", player); effect.resumesDecision = true; effects.submit(room, effect); return; }
        room.phase = "manage"; room.pending = null; log(room, player.name + " 的本次路费被免除");
    }
    /** 输入已验证的原子道具效果，修改候选房间并记录公开结果。 */
    private void resolve(Room room, GameEffect effect) {
        Player player = player(room, effect.playerId); Tile tile = tile(room, effect.tileId);
        switch (effect.type) {
            case "PLACE_BARRIER" -> { require(tile != null && tile.type.equals("property") && !tile.barrier, "路障目标不可用"); tile.barrier = true; log(room, player.name + " 在 " + tile.name + " 设置了路障"); }
            case "DEMOLISH" -> { require(tile != null && tile.ownerId != null && tile.level > 0, "建筑目标不可用"); tile.level--; log(room, tile.name + " 的建筑降低一级"); }
            case "SWAP_POSITION" -> { Player target = player(room, effect.targetId); require(target != null && !target.bankrupt && !target.id.equals(player.id), "换位目标不可用"); int position = player.position; player.position = target.position; target.position = position; log(room, player.name + " 与 " + target.name + " 交换了位置"); }
            case "WAIVE_RENT" -> { require(room.phase.equals("rent") && room.pending != null && room.pending.playerId.equals(player.id), "当前没有待支付的租金"); room.phase = "manage"; room.pending = null; log(room, player.name + " 的本次路费被免除"); }
            default -> throw new GameError("未知道具效果");
        }
    }
}
