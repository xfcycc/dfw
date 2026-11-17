package com.dfw.domain;

import java.util.*;
import static com.dfw.domain.GameError.require;
import static com.dfw.domain.GameSupport.*;

/** 「你的名字。」事件的地产估值、预览与强制换地。 */
public final class LandSwap {
    public static final String TYPE = "land_swap";
    private LandSwap() {}

    /** 自家地产、由抽牌人支付的差价（负数表示收款），以及两处地产交换后的租金和连地加成。 */
    public record Pair(int ownTileId, long ownValue, long cashDue, long receivedRent, long givenRent,
                       int receivedConnected, int givenConnected, int previousReceivedConnected, int previousGivenConnected) {}

    /** 对手地产的估值、当前租金、不可选原因及对应的可交换自家地产。 */
    public record Target(int tileId, long value, long rent, String reason, List<Pair> pairs) {}

    /** 输入房间和抽牌人，列出对手地产，并按差价从低到高返回每块地的合法配对及预览。 */
    public static List<Target> targets(Room room, Player actor) {
        var ownTiles = room.board.stream().filter(t -> actor.id.equals(t.ownerId) && blocked(room,t).isEmpty()).toList();
        List<Target> targets = new ArrayList<>();
        for (Tile target : room.board) {
            if (!"property".equals(target.type) || target.ownerId == null || actor.id.equals(target.ownerId)) continue;
            Player opponent = player(room,target.ownerId);
            if (opponent == null || opponent.bankrupt) continue;
            long value = Rules.propertyValue(room,target);
            String reason = blocked(room,target);
            List<Pair> pairs = reason.isEmpty() ? ownTiles.stream()
                    .filter(own -> canExchange(room,actor,opponent,own,target))
                    .map(own -> preview(room,actor,opponent,own,target))
                    .sorted(Comparator.comparingLong(pair -> Math.abs(pair.cashDue()))).toList() : List.of();
            if (reason.isEmpty() && pairs.isEmpty()) reason = "没有价差和现金符合条件的自家地产";
            targets.add(new Target(target.id,value,rent(room,target,opponent,PropertyEffects.connectedRentPercent(room.board,target),true),reason,pairs));
        }
        return targets;
    }

    /** 输入地产，返回不能参与换地的原因；空字符串表示地块本身可以参与。 */
    private static String blocked(Room room, Tile tile) {
        if (tile.mortgaged) return "地产已抵押";
        if (PropertyEffects.frozen(tile)) return "地产已封印";
        if (CrossoverService.protectedProperty(room,tile)) return "地产受防御特化保护";
        return "";
    }

    /** 输入两名玩家和双方地产，判断按低价估值的 10% 上限及当前现金能否结算。 */
    private static boolean canExchange(Room room, Player actor, Player opponent, Tile own, Tile target) {
        long ownValue = Rules.propertyValue(room,own), targetValue = Rules.propertyValue(room,target);
        long due = targetValue-ownValue;
        return Math.abs(due) <= Math.min(ownValue,targetValue)/10 && due <= actor.cash && -due <= opponent.cash;
    }

    /** 输入一对合法地产，按互换后的虚拟产权算两块地的租金，不改变房间。 */
    private static Pair preview(Room room, Player actor, Player opponent, Tile own, Tile target) {
        int received = PropertyEffects.connectedRentPercent(room.board,target,t -> t.id!=own.id && actor.id.equals(t.ownerId));
        int given = PropertyEffects.connectedRentPercent(room.board,own,t -> t.id!=target.id && opponent.id.equals(t.ownerId));
        long ownValue = Rules.propertyValue(room,own);
        return new Pair(own.id,ownValue,Rules.propertyValue(room,target)-ownValue,
                rent(room,target,actor,received,false),rent(room,own,opponent,given,false),received,given,
                PropertyEffects.connectedRentPercent(room.board,target),PropertyEffects.connectedRentPercent(room.board,own));
    }

    /** 输入地产及预期业主，返回不含特定付款人状态的租金预览；原业主专属的花瓣标记不会随产权转移。 */
    private static long rent(Room room, Tile tile, Player owner, int connected, boolean currentOwner) {
        long amount = Rules.rent(room,tile);
        if (amount==0 || owner.jailed && owner.skipTurns>0) return 0;
        if (currentOwner) amount = NetaService.previewPetalRent(room,tile,amount);
        return PropertyEffects.boostedRent(PropertyEffects.concertRent(amount,tile.concertStacks),tile,connected,Rules.cityRentPercent(room));
    }

    /** 输入抽牌人、原选择及答复，再核产权、资格和差价；一次更新双方现金和产权并记录结果。 */
    static void decide(Room room, Player actor, ContentChoice choice, Action action) {
        require(action.accept!=null,"请选择确认或放弃换地");
        if (!action.accept) { log(room,actor.name+" 放弃「你的名字。」换地"); return; }
        require(action.tileIds!=null && action.tileIds.size()==2,"请选择对手和自家各一处地产");
        Tile target=tile(room,action.tileIds.getFirst()), own=tile(room,action.tileIds.getLast());
        require(target!=null && own!=null && choice.tileIds.contains(target.id) && "property".equals(target.type)
                && "property".equals(own.type) && actor.id.equals(own.ownerId) && target.ownerId!=null && !actor.id.equals(target.ownerId),"请选择当前可换的对手和自家地产");
        Player opponent=player(room,target.ownerId);
        require(opponent!=null && !opponent.bankrupt && blocked(room,own).isEmpty() && blocked(room,target).isEmpty()
                && canExchange(room,actor,opponent,own,target),"当前地产或补差现金已变化，请重新选择");
        long due=Rules.propertyValue(room,target)-Rules.propertyValue(room,own);
        BoardEffect.Observation presentation=BoardEffect.watch(room,"exchange",actor);
        actor.cash-=due; opponent.cash+=due;
        own.ownerId=opponent.id; target.ownerId=actor.id;
        SettlementService.observe(room);
        log(room,"log",actor.id,actor.name+" 通过「你的名字。」用 "+own.name+"（"+own.id+"号）与 "+opponent.name+" 的 "+target.name+"（"+target.id+"号）互换产权"+
                (due==0?"，无需补差":"，"+(due>0?actor.name:opponent.name)+" 补差 ¥"+String.format("%,d",Math.abs(due))));
        presentation.finish(room);
    }
}
