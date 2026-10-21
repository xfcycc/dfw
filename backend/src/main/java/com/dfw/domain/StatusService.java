package com.dfw.domain;

import static com.dfw.domain.GameSupport.*;
import static com.dfw.domain.GameError.require;

/** 首期持续状态生命周期；陷阱免疫只作用于 TRAP，不影响租金或灾害。 */
public final class StatusService {
    /** 输入房间与状态效果，赋予或刷新陷阱免疫、一次性费用或塔塔开状态，重复不叠加。 */
    public static void add(Room room, GameEffect effect) {
        require(("TRAP_IMMUNITY".equals(effect.statusId) && effect.amount == 3) || (java.util.Set.of("NEXT_PAYMENT_WAIVER","NEXT_RENT_DOUBLE","NEXT_MOVE_DOUBLE","NEXT_ROLL_HALF").contains(effect.statusId) && effect.amount == 1), "持续状态参数不可用");
        Player owner = player(room, effect.playerId);
        if (owner == null || owner.bankrupt) return;
        PlayerStatus status = owner.statuses.stream().filter(value -> value.type.equals(effect.statusId)).findFirst().orElse(null);
        if (status == null) { status = new PlayerStatus(); status.type = effect.statusId; owner.statuses.add(status); }
        status.sourceEffectId = effect.id; status.appliedTurn = room.turnNumber; status.remainingTurns = (int)effect.amount;
        log(room, owner.name + switch(effect.statusId) { case "TRAP_IMMUNITY" -> " 获得陷阱免疫，持续三个后续个人回合，休息也计数"; case "NEXT_PAYMENT_WAIVER" -> " 获得下次强制费用免单，不叠加"; case "NEXT_ROLL_HALF" -> " 下次掷骰或替代掷骰基础步数减半，向下取整，不叠加"; case "NEXT_MOVE_DOUBLE" -> " 获得塔塔开，下次最终移动距离翻倍，不叠加，传送与换位不消耗"; default -> " 下次正租金翻倍，不叠加"; });
    }
    /** 输入玩家和效果标签，返回有效状态是否免疫该标签。 */
    public static boolean immune(Player player, String tag) {
        return "TRAP".equals(tag) && player.statuses.stream().anyMatch(value -> "TRAP_IMMUNITY".equals(value.type) && value.remainingTurns > 0);
    }
    /** 输入玩家，返回命中控制后尚在等待跳过或处于两回合保护中的剩余个人回合数。 */
    public static int controlProtectionTurns(Player player) {
        return player.statuses.stream().filter(s -> s.type.equals("CONTROL_PROTECTION")).mapToInt(s -> s.remainingTurns).max().orElse(0);
    }
    /** 输入房间和实际受控玩家，登记两回合保护，等待本次行动跳过后才开始计时。 */
    public static void protectControl(Room room,Player owner) {
        PlayerStatus status=new PlayerStatus(); status.type="CONTROL_PROTECTION"; status.remainingTurns=2; status.appliedTurn=room.turnNumber; status.awaitingControlledSkip=true;
        owner.statuses.add(status);
        log(room,owner.name+" 获得连续控制保护，本次受控行动跳过后保护两个个人回合");
    }
    /** 输入房间和刚跳过行动的玩家，启动已登记的保护，不延长已开始的期限。 */
    public static void controlledSkip(Room room,Player owner) {
        for(PlayerStatus status:owner.statuses) if(status.type.equals("CONTROL_PROTECTION") && status.awaitingControlledSkip) {
            status.awaitingControlledSkip=false; status.appliedTurn=room.turnNumber;
        }
    }
    /** 输入玩家和一次性状态类型，存在时消费并返回真，重复获得不会累加。 */
    public static boolean consume(Player owner,String type) { return owner.statuses.removeIf(s -> s.type.equals(type) && s.remainingTurns > 0); }
    /** 输入房间与结束回合的持有人，扣除后续个人回合并移除到期状态，无返回值。 */
    public static void endTurn(Room room, Player owner) {
        PropertyEffects.endTurn(room, owner);
        for (PlayerStatus status : owner.statuses) if (("TRAP_IMMUNITY".equals(status.type) || "CONTROL_PROTECTION".equals(status.type) && !status.awaitingControlledSkip) && room.turnNumber > status.appliedTurn) status.remainingTurns--;
        boolean expired = owner.statuses.removeIf(status -> {
            if (status.remainingTurns > 0) return false;
            log(room, owner.name + (status.type.equals("CONTROL_PROTECTION") ? " 的连续控制保护已到期" : " 的陷阱免疫已到期")); return true;
        });
        if (expired) BoardEffect.cue(room,"statusExpired",owner);
    }
}
