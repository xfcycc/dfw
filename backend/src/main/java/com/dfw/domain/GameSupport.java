package com.dfw.domain;

import java.util.*;

/** 领域内公开记录和对象查找。 */
public final class GameSupport {
    /** 输入房间和玩家标识，返回玩家或空。 */
    public static Player player(Room room, String id) { return room.players.stream().filter(p -> Objects.equals(p.id, id)).findFirst().orElse(null); }
    /** 输入房间，返回当前行动者或空。 */
    public static Player current(Room room) { return room.turnOrder.isEmpty() ? null : player(room, room.turnOrder.get(room.turnIndex)); }
    /** 输入房间和公开文案，追加最多一百条公开事件。 */
    public static void log(Room room, String text) {
        PublicLog entry = new PublicLog(); entry.id = UUID.randomUUID().toString(); entry.text = text; entry.at = System.currentTimeMillis();
        room.logs.add(0, entry); if (room.logs.size() > 100) room.logs.removeLast();
    }
    /** 输入房间和索引，返回地块或空。 */
    public static Tile tile(Room room, Integer id) { return id == null || id < 0 || id >= room.board.size() ? null : room.board.get(id); }
    /** 输入决策参数，返回完整待处理事项。 */
    public static PendingDecision pending(String type, Player player, Integer tileId, long amount, String creditorId) {
        PendingDecision result = new PendingDecision(); result.type = type; result.playerId = player.id; result.tileId = tileId; result.amount = amount; result.creditorId = creditorId; return result;
    }
}
