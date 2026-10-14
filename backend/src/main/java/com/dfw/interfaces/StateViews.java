package com.dfw.interfaces;

import com.dfw.domain.*;
import com.dfw.interfaces.vo.*;
import static com.dfw.domain.GameSupport.*;

/** 显式白名单投影；内部房间不得直接成为 WebSocket payload。 */
public final class StateViews {
    /** 输入内部房间，返回不含手牌、种子或内部报价对象的公开快照。 */
    public static PublicRoomVo publicRoom(Room room) {
        PublicRoomVo value = new PublicRoomVo(); value.code = room.code; value.version = room.version; value.status = room.status; value.hostId = room.hostId; value.gameId = room.gameId;
        value.roomId = room.roomId; value.recoveryPending = room.recoveryPending; value.recovered = room.recovered; value.sourceRoomId = room.sourceRoomId; value.sourceSnapshotId = room.sourceSnapshotId;
        value.mapSize = room.mapSize == null ? "large" : room.mapSize; value.economy = Rules.economy(room); value.board = room.board; value.turnOrder = room.turnOrder; value.turnIndex = room.turnIndex; value.turnNumber = room.turnNumber; value.phase = room.phase;
        value.chanceEventDescriptions = room.chanceEventDescriptions; value.upgradedTiles=room.upgradedTiles;
        if(room.runtime!=null && room.runtime.lastContent!=null && !room.runtime.lastContent.privateSupply) { value.event=room.runtime.lastContent; value.eventPlayerId=room.runtime.contentPlayerId; }
        if(room.runtime!=null) { value.playedCard=room.runtime.playedCard; value.playedCardPlayerId=room.runtime.playedCardPlayerId; }
        value.pending = room.pending; value.tradePending = room.trade != null; value.pausedReason = room.pausedReason; value.dice = room.dice; value.motion = room.motion; value.winnerId = room.winnerId; value.logs = room.logs;
        var assets = new java.util.HashMap<String, Long>();
        for (Player player : room.players) assets.put(player.id, Rules.netWorth(room, player));
        for (Player player : room.players) {
            PublicPlayerVo p = new PublicPlayerVo(); p.id = player.id; p.name = player.name; p.color = player.color; p.avatarId = player.avatarId == null ? PlayerAvatars.DEFAULT : player.avatarId; p.cash = player.cash; p.position = player.position;
            p.cardCount = player.hand.size();
            p.netWorth = assets.get(player.id); p.assetRank = player.bankrupt ? null : 1 + (int) room.players.stream().filter(other -> !other.bankrupt && assets.get(other.id) > p.netWorth).count();
            p.ready = player.ready; p.online = player.online; p.bankrupt = player.bankrupt; p.skipTurns = player.skipTurns; p.jailed = player.jailed; p.eliminationOrder = player.eliminationOrder;
            p.nextPaymentWaiver=player.statuses.stream().anyMatch(s -> s.type.equals("NEXT_PAYMENT_WAIVER"));
            p.nextRentDouble=player.statuses.stream().anyMatch(s -> s.type.equals("NEXT_RENT_DOUBLE"));
            p.trapImmunityTurns = player.statuses.stream().filter(status -> "TRAP_IMMUNITY".equals(status.type)).mapToInt(status -> status.remainingTurns).max().orElse(0); value.players.add(p);
        }
        return value;
    }
    /** 输入内部房间和本人标识，返回本人手牌及参与的报价。 */
    public static PrivatePlayerVo privatePlayer(Room room, String id) {
        PrivatePlayerVo value = new PrivatePlayerVo(); value.playerId = id; value.hand = player(room, id).hand;
        value.archiveCode = room.archiveCode;
        if (id.equals(room.hostId)) for (SeatClaim claim : room.seatClaims) { SeatClaimVo v = new SeatClaimVo(); v.id = claim.id; v.playerId = claim.playerId; v.name = claim.name; v.at = claim.at; value.seatClaims.add(v); }
        Player owner=player(room,id); value.notice=owner.contentNotice; value.noticeText=owner.contentNoticeText; value.tradeRejection=owner.tradeRejection;
        if(room.runtime!=null) {
            if(room.runtime.lastContent!=null && room.runtime.lastContent.privateSupply && id.equals(room.runtime.contentPlayerId)) value.event=room.runtime.lastContent;
            ContentChoice choice=room.runtime.choice;
            if(choice!=null && choice.playerId.equals(id)) {
                ContentChoiceVo v=new ContentChoiceVo(); v.id=choice.id; v.type=choice.type; v.tileIds=choice.tileIds; v.cards=choice.cards; v.cardId=choice.cardId;
                v.content=choice.content; v.skipTurns=choice.skipTurns; v.jailed=choice.jailed; v.discardCash=choice.discardCash; value.choice=v;
            }
        }
        value.activeCardUsed = current(room) != null && current(room).id.equals(id) && room.activeCardUsed;
        value.trade = room.trade != null && (room.trade.fromId.equals(id) || room.trade.toId.equals(id)) ? room.trade : null; return value;
    }
}
