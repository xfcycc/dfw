package com.dfw.interfaces;

import com.dfw.domain.*;
import com.dfw.interfaces.vo.*;
import static com.dfw.domain.GameSupport.*;

/** 显式白名单投影；内部房间不得直接成为 WebSocket payload。 */
public final class StateViews {
    /** 输入内部房间，返回不含手牌、种子或报价内容的公开快照。 */
    public static PublicRoomVo publicRoom(Room room) {
        PublicRoomVo value = new PublicRoomVo(); value.code = room.code; value.version = room.version; value.status = room.status; value.hostId = room.hostId; value.gameId = room.gameId;
        value.board = room.board; value.turnOrder = room.turnOrder; value.turnIndex = room.turnIndex; value.turnNumber = room.turnNumber; value.phase = room.phase;
        value.chanceEventDescriptions = room.chanceEventDescriptions;
        value.pending = room.pending; value.tradePending = room.trade != null; value.pausedReason = room.pausedReason; value.dice = room.dice; value.motion = room.motion; value.winnerId = room.winnerId; value.logs = room.logs;
        for (Player player : room.players) {
            PublicPlayerVo p = new PublicPlayerVo(); p.id = player.id; p.name = player.name; p.color = player.color; p.cash = player.cash; p.position = player.position;
            p.ready = player.ready; p.online = player.online; p.bankrupt = player.bankrupt; p.skipTurns = player.skipTurns; p.jailed = player.jailed; p.eliminationOrder = player.eliminationOrder;
            p.trapImmunityTurns = player.statuses.stream().filter(status -> "TRAP_IMMUNITY".equals(status.type)).mapToInt(status -> status.remainingTurns).max().orElse(0); value.players.add(p);
        }
        return value;
    }
    /** 输入内部房间和本人标识，返回本人手牌及参与的报价。 */
    public static PrivatePlayerVo privatePlayer(Room room, String id) {
        PrivatePlayerVo value = new PrivatePlayerVo(); value.playerId = id; value.hand = player(room, id).hand;
        value.activeCardUsed = current(room) != null && current(room).id.equals(id) && room.activeCardUsed;
        value.trade = room.trade != null && (room.trade.fromId.equals(id) || room.trade.toId.equals(id)) ? room.trade : null; return value;
    }
}
