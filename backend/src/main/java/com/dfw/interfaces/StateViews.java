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
        value.activityRotation = rotation(room); value.activity = activity(room);
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
            p.nextMoveDouble=player.statuses.stream().anyMatch(s -> s.type.equals("NEXT_MOVE_DOUBLE") && s.remainingTurns>0);
            p.trapImmunityTurns = player.statuses.stream().filter(status -> "TRAP_IMMUNITY".equals(status.type)).mapToInt(status -> status.remainingTurns).max().orElse(0); value.players.add(p);
        }
        return value;
    }
    /** 输入内部房间和本人标识，返回本人手牌及参与的报价。 */
    public static PrivatePlayerVo privatePlayer(Room room, String id) {
        PrivatePlayerVo value = new PrivatePlayerVo(); value.playerId = id; value.hand = player(room, id).hand;
        value.archiveCode = room.archiveCode;
        if (id.equals(room.hostId)) for (SeatClaim claim : room.seatClaims) { SeatClaimVo v = new SeatClaimVo(); v.id = claim.id; v.playerId = claim.playerId; v.name = claim.name; v.at = claim.at; value.seatClaims.add(v); }
        if (room.activity != null) { value.activityId=room.activity.id; value.activityRound=room.activity.round; value.activitySelection=room.activity.choices.get(id); }
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
    /** 输入房间，返回不含随机队列的轮换预告。 */
    private static ActivityRotationVo rotation(Room room) {
        ActivityRotation r=room.activityRotation; if(r==null) return null;
        ActivityRotationVo v=new ActivityRotationVo(); v.period=r.period; v.completedTurns=r.completedTurns; v.revision=r.revision; v.tileIds=r.tileIds; v.current=r.current; v.next=r.next; v.money=r.money; return v;
    }
    /** 输入房间，按揭晓阶段显式白名单投影，绝不返回子弹位置或未揭晓随机结果。 */
    private static ActivityVo activity(Room room) {
        CornerActivity a=room.activity; if(a==null) return null;
        ActivityVo v=new ActivityVo(); v.id=a.id; v.kind=a.kind; v.tileId=a.tileId; v.triggerId=a.triggerId; v.stage=a.stage; v.round=a.round; v.participants=a.participants; v.contenders=a.contenders; v.submitted=new java.util.ArrayList<>(a.choices.keySet());
        if(a.kind.equals("horse")) v.bets=a.choices;
        v.revealed=a.revealed; v.wagers=a.wagers; v.payouts=a.payouts; v.currentPlayerId=a.currentPlayerId; v.opponentId=a.opponentId; v.lastShooterId=a.lastShooterId; v.shots=a.shots;
        v.level=a.level; v.safePaths=a.safePaths;
        v.startedAt=a.startedAt; v.endsAt=a.endsAt; v.settled=a.settled; v.summary=a.summary;
        if(a.settled && (a.kind.equals("casino") || a.kind.equals("horse"))) v.result=a.sample;
        if(a.kind.equals("packet")) {
            v.packetSelections.putAll(a.packetSelections);
            if(v.packetSelections.isEmpty()) for(String id:a.choices.keySet()) v.packetSelections.put(id,a.participants.indexOf(id)+1);
            for(var claim:v.packetSelections.entrySet()) if(a.payouts.containsKey(claim.getKey())) {
                String slot=a.participants.get(claim.getValue()-1);
                v.packets.put(claim.getKey(),a.packets.get(slot)); v.bonuses.put(claim.getKey(),a.bonuses.get(slot));
            }
        }
        if(a.kind.equals("blindbox")) {
            v.packetSelections.putAll(a.packetSelections);
            for(String id:a.payouts.keySet()) { v.packets.put(id,a.packets.get(id)); v.bonuses.put(id,a.bonuses.get(id)); }
        }
        return v;
    }
}
