package com.dfw.domain;

import java.util.*;
import static com.dfw.domain.GameSupport.*;
import static com.dfw.domain.GameError.require;

/** 地产变现、债务和淘汰规则。 */
public class EconomyService {
    private final RandomSource random;
    /** 输入随机源，用于固定每笔待支付费用的身份。 */
    public EconomyService(RandomSource random) { this.random = random; }
    /** 输入房间和玩家，返回现金加各地产最高银行变现所得，已抵押地产仅计正净额。 */
    public long liquidationValue(Room room, Player player) {
        long value = player.cash;
        for (Tile tile : room.board) if (Objects.equals(tile.ownerId, player.id) && !PropertyEffects.frozen(tile)) value += Rules.propertyLiquidationValue(room, tile);
        return value;
    }
    /** 输入房间、被清算人、债权及房主移出标记，先冻结个人战报，再清算并由回合服务切换行动者。 */
    public void eliminate(Room room, Player player, String creditorId, long debt, boolean removed) {
        Player creditor = player(room, creditorId);
        PendingDecision payment = paymentPending(room) && room.pending.playerId.equals(player.id) ? room.pending : null;
        if (creditor != null && !creditor.bankrupt) {
            long available = player.cash + room.board.stream().filter(t -> Objects.equals(t.ownerId,player.id)).mapToLong(t -> Rules.propertyLiquidationValue(room,t)).sum();
            long paid = Math.min(available, debt);
            creditor.cash += paid; SettlementService.payment(room, player, payment, paid);
        }
        SettlementService.eliminate(room, player, payment, removed);
        for (Tile tile : room.board) if (Objects.equals(tile.ownerId, player.id)) { tile.ownerId = null; tile.level = 0; tile.mortgaged = false; tile.mortgageBase = null; }
        player.cash = 0; player.contentNotice=null; player.contentNoticeText=null; player.hand.clear(); player.statuses.clear(); player.bankrupt = true; player.skipTurns = 0; player.jailed = false;
        PropertyEffects.eliminateCaster(room, player.id);
        player.eliminationOrder = (int) room.players.stream().filter(p -> p.bankrupt).count();
        if (room.trade != null && (room.trade.fromId.equals(player.id) || room.trade.toId.equals(player.id))) room.trade = null;
        if (room.runtime != null && room.runtime.choice != null && room.runtime.choice.returnPending != null) {
            PendingDecision parent=room.runtime.choice.returnPending;
            if(Objects.equals(parent.playerId,player.id)) { room.runtime.choice.returnPending=null; room.runtime.choice.returnPhase="manage"; }
            else if(Objects.equals(parent.creditorId,player.id)) parent.creditorId=null;
        }
        if (room.runtime != null && room.runtime.choice != null && Objects.equals(room.runtime.choice.playerId,player.id)) {
            ContentChoice choice=room.runtime.choice; room.pending=choice.returnPending; room.phase=choice.returnPhase; room.runtime.choice=null;
        }
        if (room.pending != null && Objects.equals(room.pending.playerId, player.id)) room.pending = null;
        if (room.pending != null && Objects.equals(room.pending.creditorId, player.id)) { room.pending.creditorId = null; room.trade = null; }
        log(room, player.name + " 已清算并退出竞争");
    }
    /** 输入强制费用及付款人，保存支付选择；零费用或房东服刑期间的租金直接完成，不消耗防御状态。 */
    public void charge(Room room, Player player, long amount, String creditorId, Integer tileId) {
        require(amount >= 0 && room.pending == null, "费用结算上下文不可用");
        if (amount == 0) return;
        Player landlord = player(room, creditorId);
        if (tileId != null && landlord != null && landlord.jailed && landlord.skipTurns > 0) {
            log(room, "log", player.id, landlord.name + " 正在服刑，" + player.name + " 本次免付 " + tile(room, tileId).name + " 的租金"); return;
        }
        if (tileId != null && creditorId != null) {
            Tile property = tile(room,tileId);
            if (PropertyEffects.frozen(property) || AnimeService.iced(property)) return;
            if (property.concertStacks > 0) {
                BoardEffect.Observation presentation = BoardEffect.watch(room,"concertRent",player);
                amount = PropertyEffects.concertRent(amount,property.concertStacks);
                log(room,property.name + " 消耗 " + property.concertStacks + " 层演唱会，租金变为 ¥" + amount); property.concertStacks=0;
                presentation.finish(room);
            }
        }
        if (tileId != null && creditorId != null && StatusService.consume(player, "NEXT_RENT_DOUBLE")) { amount = Math.multiplyExact(amount,2); log(room,"log",player.id,player.name+" 本次正租金翻倍至 ¥"+String.format("%,d",amount)+"，状态已消耗"); BoardEffect.cue(room,"rentDouble",player); }
        String type = tileId != null && creditorId != null ? "rent" : "debt";
        PendingDecision payment = pending(type, player, tileId, amount, creditorId);
        if (StatusService.consume(player,"NEXT_PAYMENT_WAIVER")) { log(room,"log",player.id,player.name+" 自动免除 "+paymentDescription(room,payment)+" ¥"+String.format("%,d",amount)+"，剩余待支付 ¥0，状态已消耗"); BoardEffect.cue(room,"waiver",player); return; }
        room.pending = payment; room.pending.id = random.id(); room.pending.originalAmount = amount; room.phase = type;
        log(room, "log", player.id, player.name + " 待支付 " + paymentDescription(room, room.pending) + " ¥" + String.format("%,d", amount) + "，请选择支付方式");
    }
    /** 输入付款人，确认用现金支付当前费用余额并恢复父流程。 */
    public void pay(Room room, Player player) {
        require(paymentPending(room) && room.pending.playerId.equals(player.id), "当前没有你的待支付费用");
        PendingDecision debt = room.pending;
        require(debt.defenseResolved, "请先确认付款防御");
        require(player.cash >= debt.amount, "现金不足，请先抵押、出售资产或协商抵债");
        player.cash -= debt.amount; Player creditor = player(room, debt.creditorId);
        if (creditor != null && !creditor.bankrupt) creditor.cash += debt.amount;
        SettlementService.payment(room, player, debt, debt.amount);
        log(room, "log", player.id, player.name + " 用现金支付 " + paymentDescription(room, debt) + " ¥" + String.format("%,d", debt.amount) + "，本笔欠款已结清");
        room.pending = null; room.phase = "manage";
    }
    /** 输入债务人，按最高银行变现能力校验清算；银行费用仍须先自行抵押或出售可抵押地产。 */
    public void declareBankruptcy(Room room, Player player) {
        require(paymentPending(room) && room.pending.playerId.equals(player.id), "当前没有你的待支付费用");
        PendingDecision debt = room.pending;
        require(liquidationValue(room, player) < debt.amount, "现金加可变现资产足以偿清，请选择资产筹款并支付");
        require(debt.creditorId != null || room.board.stream().noneMatch(value -> Objects.equals(value.ownerId, player.id) && !value.mortgaged && !PropertyEffects.frozen(value)), "系统费用须先自行抵押或出售全部可抵押地产");
        eliminate(room, player, debt.creditorId, debt.amount, false);
    }
    /** 输入房间、玩家和地块索引，返回本人地产。 */
    public Tile ownedTile(Room room, Player player, Integer id) {
        Tile tile = tile(room, id); require(tile != null && tile.type.equals("property") && Objects.equals(tile.ownerId, player.id), "这块地产不属于你"); return tile;
    }
    /** 输入房间、操作人、经营操作及地产索引，结算抵押、赎回、卖房或整售；整售先结清固定赎回费，欠款仍需手动支付。 */
    public void manage(Room room, Player player, String action, int id) {
        require(!paymentPending(room) || !action.equals("redeem"), "费用结算期间不可赎回地产");
        Tile tile = ownedTile(room, player, id);
        require(!PropertyEffects.frozen(tile), "神威空间中的地产无法操作");
        String land = tile.name + "（" + tile.id + "号）";
        switch (action) {
            case "sellBuilding" -> {
                require(tile.level > 0 && !tile.mortgaged, "地产没有可出售建筑，或建筑已随地产抵押"); tile.level--;
                tile.damageOwnerId=null; tile.damageLevel=0; tile.damageUntilTurn=0;
                long cash = Rules.sellBuildingValue(room, tile); player.cash += cash;
                log(room, "log", player.id, player.name + " 出售 " + land + " 的一级建筑，建筑从 " + (tile.level + 1) + " 级降至 " + tile.level + " 级，银行返款 ¥" + String.format("%,d", cash) + "，现金余额 ¥" + String.format("%,d", player.cash));
            }
            case "mortgage" -> {
                require(!tile.mortgaged, "不可重复抵押"); tile.mortgageBase = Rules.propertyValue(room, tile); tile.mortgaged = true;
                long cash = Rules.mortgageValue(room, tile); player.cash += cash;
                log(room, "log", player.id, player.name + " 整体抵押 " + land + "，从银行取得 ¥" + String.format("%,d", cash) + "，赎回需 ¥" + String.format("%,d", Rules.redeemCost(tile)) + "，保留 " + tile.level + " 级建筑并暂停收租，现金余额 ¥" + String.format("%,d", player.cash));
            }
            case "redeem" -> {
                long cost = Rules.redeemCost(tile); require(tile.mortgaged && player.cash >= cost, "地产未抵押或赎回现金不足");
                player.cash -= cost; tile.mortgaged = false; tile.mortgageBase = null;
                log(room, "log", player.id, player.name + " 赎回 " + land + "，支付银行 ¥" + String.format("%,d", cost) + "，保留 " + tile.level + " 级建筑并恢复收租，现金余额 ¥" + String.format("%,d", player.cash));
            }
            case "sellLand" -> {
                long cost = tile.mortgaged ? Rules.redeemCost(tile) : 0, cash = Rules.landValue(room, tile);
                require(cash >= 0 || player.cash >= -cash, "回收款不足结清固定赎回费用，现金不足补差价");
                int level = tile.level; player.cash += cash;
                tile.ownerId = null; tile.level = 0; tile.mortgaged = false; tile.mortgageBase = null;
                log(room, "log", player.id, player.name + " 整套出售 " + land + "（含 " + level + " 级建筑），银行回收款 ¥" + String.format("%,d", cash + cost) + (cost > 0 ? "，扣除固定赎回费用 ¥" + String.format("%,d", cost) : "") + (cash >= 0 ? "，实际到账 ¥" + String.format("%,d", cash) : "，补付差价 ¥" + String.format("%,d", -cash)) + "，建筑清除，地产恢复为待购入，现金余额 ¥" + String.format("%,d", player.cash));
            }
            default -> throw new GameError("当前操作不可用");
        }
    }
}
