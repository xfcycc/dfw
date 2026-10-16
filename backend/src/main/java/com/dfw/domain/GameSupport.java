package com.dfw.domain;

import java.util.*;

/** 领域内公开记录和对象查找。 */
public final class GameSupport {
    /** 输入房间和玩家标识，返回玩家或空。 */
    public static Player player(Room room, String id) { return room.players.stream().filter(p -> Objects.equals(p.id, id)).findFirst().orElse(null); }
    /** 输入房间，返回当前行动者或空。 */
    public static Player current(Room room) { return room.turnOrder.isEmpty() ? null : player(room, room.turnOrder.get(room.turnIndex)); }
    /** 输入房间和公开文案，追加最多一百条快照动态，同时保存完整事实。 */
    public static void log(Room room, String text) { log(room, "log", null, text); }
    /** 输入公开事实类型、作用玩家及文案，保存日志身份便于实时记录去重。 */
    public static void log(Room room, String type, String actorId, String text) {
        PublicLog entry = new PublicLog(); entry.id = UUID.randomUUID().toString(); entry.type = type; entry.text = text; entry.at = System.currentTimeMillis();
        for (Player player : room.players) if (text.contains(player.name)) { PlayerMention mention = new PlayerMention(); mention.id = player.id; mention.name = player.name; mention.color = player.color; entry.mentions.add(mention); }
        room.logs.add(0, entry); if (room.logs.size() > 100) room.logs.removeLast();
        HistoryTrace.event(room, type, actorId, text, Map.of("logId", entry.id, "mentions", entry.mentions));
    }
    /** 输入已使用手牌及目标，保留皮肤身份和效果供公开揭卡，不公开剩余手牌或抢夺结果。 */
    public static void cardLog(Room room, Player owner, Card card, Action action) {
        Player opponent = player(room, action.playerId); Tile target = tile(room, action.tileId);
        String aim = opponent != null ? opponent.name : target != null ? target.name + "（" + target.id + "号）" : owner.name;
        String effect = card.effect;
        if (effect == null) effect = switch (card.kind) {
            case "remote" -> "指定 " + action.point + " 点移动";
            case "barrier" -> "设置一次路障，首个受影响的经过者截停并正常结算落点";
            case "stay" -> "原地重新结算落点";
            case "immunity" -> "免除本笔租金";
            case "demolish" -> "建筑降低一级，无退款";
            case "swap" -> "交换位置，随后正常掷骰";
            default -> "发动对应效果";
        };
        if (card.kind.equals("barrier")) effect = "在前方六格内地块设置一次路障，首个受影响者截停并正常结算落点";
        if (card.kind.equals("remote")) effect = "指定 " + action.point + " 点移动，替代本次掷骰";
        ContentResult played = new ContentResult(); played.id = card.id; played.pool = "card"; played.kind = card.kind;
        played.skinId = card.skinId;
        played.name = card.name; played.rarity = card.rarity; played.cardType = card.cardType;
        played.quote = card.quote; played.description = card.description; played.effect = effect;
        room.runtime.playedCard = played; room.runtime.playedCardPlayerId = owner.id;
        log(room, "card_used", owner.id, owner.name + " 使用道具「" + (card.name == null ? switch (card.kind) { case "remote" -> "遥控骰子"; case "barrier" -> "路障"; case "stay" -> "停留"; case "immunity" -> "免租"; case "demolish" -> "拆房"; case "swap" -> "换位"; default -> card.kind; } : card.name) + "」；目标：" + aim + "；效果：" + effect);
    }
    /** 输入房间和索引，返回地块或空。 */
    public static Tile tile(Room room, Integer id) { return id == null || id < 0 || id >= room.board.size() ? null : room.board.get(id); }
    /** 输入房间，返回当前待选是否为强制费用结算。 */
    public static boolean paymentPending(Room room) { return room.pending != null && (room.pending.type.equals("rent") || room.pending.type.equals("debt")); }
    /** 输入房间和待支付费用，返回费用类别、关联地产及实际收款方的公开说明。 */
    public static String paymentDescription(Room room, PendingDecision payment) {
        Player creditor = player(room, payment.creditorId); Tile target = tile(room, payment.tileId);
        String name = "rent".equals(payment.type) ? (target == null ? "房租" : "「" + target.name + "（" + target.id + "号）」房租") : payment.creditorId == null ? "系统费用" : "玩家债务";
        return name + "（收款方：" + (creditor != null && !creditor.bankrupt ? creditor.name : "银行") + "）";
    }
    /** 输入决策参数，返回完整待处理事项。 */
    public static PendingDecision pending(String type, Player player, Integer tileId, long amount, String creditorId) {
        PendingDecision result = new PendingDecision(); result.type = type; result.playerId = player.id; result.tileId = tileId; result.amount = amount; result.creditorId = creditorId; return result;
    }
}
