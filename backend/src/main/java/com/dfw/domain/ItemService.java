package com.dfw.domain;

import static com.dfw.domain.GameError.require;
import static com.dfw.domain.GameSupport.*;
import com.dfw.domain.rules.RuleRuntime;

/** 六种道具的验证和消耗。 */
public class ItemService {
    private final RuleRuntime ruleRuntime;
    /** 输入结算器和配置解释器，注册四种原子道具效果。 */
    public ItemService(EffectResolver effects, RuleRuntime ruleRuntime) {
        this.ruleRuntime = ruleRuntime;
        effects.register("PLACE_BARRIER", this::resolve);
        effects.register("DEMOLISH", this::resolve);
        effects.register("SWAP_POSITION", this::resolve);
        effects.register("WAIVE_RENT", this::resolve);
    }
    /** 输入房间、玩家和道具操作，按当前配置验证、消耗并入队。 */
    public void use(Room room, Player player, Action action) { ruleRuntime.useCard(room, player, action); }
    /** 输入收费玩家，按当前配置消耗一张租金防御卡并解决本次费用。 */
    public void immune(Room room, Player player) { ruleRuntime.immune(room, player); }
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
