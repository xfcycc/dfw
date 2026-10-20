package com.dfw.domain;

import static com.dfw.domain.GameError.require;
import static com.dfw.domain.GameSupport.*;
import com.dfw.domain.rules.RuleRuntime;

/** 道具效果执行及旧手牌兼容入口。 */
public class ItemService {
    private final RuleRuntime ruleRuntime;
    private final ContentService content;
    /** 输入结算器和配置解释器，注册四种原子道具效果。 */
    public ItemService(EffectResolver effects, RuleRuntime ruleRuntime, ContentService content) {
        this.ruleRuntime = ruleRuntime; this.content = content;
        effects.register("PLACE_BARRIER", this::resolve);
        effects.register("DEMOLISH", this::resolve);
        effects.register("SWAP_POSITION", this::resolve);
        effects.register("WAIVE_RENT", this::resolve);
    }
    /** 输入房间、玩家和道具操作，按当前配置验证、消耗并入队。 */
    public void use(Room room, Player player, Action action) { if (player.hand.stream().anyMatch(c -> c.id.equals(action.cardId) && (c.skinId != null || AnimeService.attacks(c.kind)))) content.use(room, player, action); else ruleRuntime.useCard(room, player, action); }
    /** 输入付款选择，旧免租卡保持已配置效果组合，新卡按取得时冻结的参数执行。 */
    public void defense(Room room, Player player, Action action) {
        Card legacy=player.hand.stream().filter(c -> c.id.equals(action.cardId) && c.skinId==null && c.kind.equals("immunity")).findFirst().orElse(null);
        if(legacy==null) { content.defense(room,player,action); return; }
        require(paymentPending(room) && room.pending.type.equals("rent") && !room.pending.defenseResolved && room.pending.playerId.equals(player.id), "本笔租金已完成防御选择");
        require(action.decisionId==null || java.util.Objects.equals(action.decisionId,room.pending.id), "费用选择已失效");
        room.pending.defenseResolved=true; ruleRuntime.immune(room,player,legacy.id);
    }
    /** 输入已验证的原子道具效果，修改候选房间并记录公开结果。 */
    private void resolve(Room room, GameEffect effect) {
        Player player = player(room, effect.playerId); Tile tile = tile(room, effect.tileId);
        switch (effect.type) {
            case "PLACE_BARRIER" -> { require(tile != null && !PropertyEffects.frozen(tile) && !tile.barrier, "路障目标不可用"); tile.barrier = true; log(room, player.name + " 在 " + tile.name + " 设置了路障"); }
            case "DEMOLISH" -> { require(tile != null && tile.ownerId != null && tile.level > 0, "建筑目标不可用"); if (!PropertyEffects.preventDemolition(room, tile)) { AnimeService.recordDamage(room,tile); tile.level--; log(room, tile.name + " 的建筑降低一级"); } }
            case "SWAP_POSITION" -> {
                Player target = player(room, effect.targetId); require(target != null && !target.bankrupt && !target.id.equals(player.id), "换位目标不可用");
                MotionSegment segment = BoardEffect.begin("swap", player);
                BoardEffect.movement(segment, player, target.position); BoardEffect.movement(segment, target, player.position);
                int position = player.position; player.position = target.position; target.position = position;
                BoardEffect.publish(room, segment); log(room, player.name + " 与 " + target.name + " 交换了位置");
            }
            case "WAIVE_RENT" -> { require(room.phase.equals("rent") && room.pending != null && room.pending.playerId.equals(player.id), "当前没有待支付的租金"); log(room, "log", player.id, player.name + " 免除 " + paymentDescription(room, room.pending) + " ¥" + String.format("%,d", room.pending.amount) + "，剩余待支付 ¥0"); room.phase = "manage"; room.pending = null; }
            default -> throw new GameError("未知道具效果");
        }
    }
}
