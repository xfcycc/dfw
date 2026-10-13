package com.dfw.domain;

import static com.dfw.domain.GameSupport.*;
import static com.dfw.domain.GameError.require;

/** 首期持续状态生命周期；陷阱免疫只作用于 TRAP，不影响租金或灾害。 */
public final class StatusService {
    /** 输入房间和已解析效果，立即赋予或刷新三个后续个人回合的陷阱免疫。 */
    public static void add(Room room, GameEffect effect) {
        require("TRAP_IMMUNITY".equals(effect.statusId) && effect.amount == 3, "持续状态参数不可用");
        Player owner = player(room, effect.playerId);
        if (owner == null || owner.bankrupt) return;
        PlayerStatus status = owner.statuses.stream().filter(value -> value.type.equals(effect.statusId)).findFirst().orElse(null);
        if (status == null) { status = new PlayerStatus(); status.type = effect.statusId; owner.statuses.add(status); }
        status.sourceEffectId = effect.id; status.appliedTurn = room.turnNumber; status.remainingTurns = 3;
        log(room, owner.name + " 获得陷阱免疫，持续三个后续个人回合，休息也计数");
    }
    /** 输入玩家和效果标签，返回有效状态是否免疫该标签。 */
    public static boolean immune(Player player, String tag) {
        return "TRAP".equals(tag) && player.statuses.stream().anyMatch(value -> "TRAP_IMMUNITY".equals(value.type) && value.remainingTurns > 0);
    }
    /** 输入房间与结束回合的持有人，扣除后续个人回合并移除到期状态，无返回值。 */
    public static void endTurn(Room room, Player owner) {
        for (PlayerStatus status : owner.statuses) if (room.turnNumber > status.appliedTurn) status.remainingTurns--;
        owner.statuses.removeIf(status -> {
            if (status.remainingTurns > 0) return false;
            log(room, owner.name + " 的陷阱免疫已到期"); return true;
        });
    }
}
