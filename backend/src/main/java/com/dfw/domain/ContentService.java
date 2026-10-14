package com.dfw.domain;

import com.dfw.domain.rules.ContentCatalog;
import java.util.*;
import static com.dfw.domain.GameError.require;
import static com.dfw.domain.GameSupport.*;

/** 完整内容的抽取、私人选择和主动/防御发动，复用已持久化的原子效果队列。 */
public final class ContentService {
    private final ContentCatalog catalog;
    private final EffectResolver effects;
    private final RandomSource random;
    /** 输入已校验目录及结算器，注册需要选择的内容效果。 */
    public ContentService(ContentCatalog catalog, EffectResolver effects, RandomSource random) {
        this.catalog = catalog; this.effects = effects; this.random = random;
        effects.register("CONTENT_EVENT", this::resolveEvent);
        effects.register("FREE_UPGRADE", this::freeUpgrade);
        effects.register("SELF_DESTROY", this::selfDestroy);
        effects.register("GAMBLE", this::gamble);
    }
    /** 输入新局房间，将全部内容现金常量冻结到其经济快照。 */
    public void freeze(Room room) { catalog.freeze(room); }
    /** 输入补给参数，抽取完整卡面并加入手牌；只有持有人收到具体卡和数量。 */
    public void giveCards(Room room, Player owner, int count, String kind) {
        for (int i = 0; i < count; i++) {
            ContentResult result = catalog.draw(room,"card",kind,random); Card card = card(result); owner.hand.add(card);
            owner.contentNotice = card; owner.contentNoticeText = "收到道具补给";
        }
    }
    /** 输入已抽到结果，生成真实手牌实体，保留所有冻结数据。 */
    private static Card card(ContentResult r) {
        Card c = new Card(); c.id=r.id; c.pool=r.pool; c.mechanism=r.mechanism; c.parameterId=r.parameterId; c.skinId=r.skinId;
        c.name=r.name; c.rarity=r.rarity; c.quote=r.quote; c.description=r.description; c.cardType=r.cardType; c.effect=r.effect; c.parameters=new LinkedHashMap<>(r.parameters); c.kind=r.mechanism; return c;
    }
    /** 输入房间，查找超过上限的旧存档或新补给；保存父费用等待，不覆盖交易或另一选择。 */
    public boolean overflow(Room room) {
        if (room.trade != null || room.runtime.choice != null || !room.status.equals("playing")) return false;
        Player owner = room.players.stream().filter(p -> !p.bankrupt && p.hand.size() > 6).findFirst().orElse(null);
        if (owner == null) return false;
        ContentChoice choice = open(room,owner,"hand_limit"); choice.cards.addAll(owner.hand);
        choice.discardCash = Rules.economy(room).money("handDiscard",200); return true;
    }
    /** 输入房间，返回是否需继续整理超限手牌，连接恢复时交给原流程推进。 */
    public boolean needsOverflow(Room room) { return room.trade == null && room.runtime.choice == null && room.players.stream().anyMatch(p -> !p.bankrupt && p.hand.size() > 6); }
    /** 输入落格玩家和池，抽一次完整事件并锁定排名目标，入队后不在等待或恢复时重抽。 */
    public void event(Room room, Player owner, String pool) {
        ContentResult result = catalog.draw(room,pool,null,random);
        if (Set.of("rich_fee","bottom_income","bottom_bonus").contains(result.mechanism)) {
            List<Player> alive=room.players.stream().filter(p -> !p.bankrupt).toList();
            Map<String,Long> worth=new HashMap<>(); alive.forEach(p -> worth.put(p.id,Rules.netWorth(room,p)));
            long extreme = result.mechanism.equals("rich_fee") ? Collections.max(worth.values()) : Collections.min(worth.values());
            List<Player> candidates=alive.stream().filter(p -> worth.get(p.id)==extreme).toList();
            result.targetId=candidates.get(random.nextInt(candidates.size())).id;
        }
        room.runtime.lastContent=result; room.runtime.contentPlayerId=owner.id;
        HistoryTrace.event(room, "event_drawn", owner.id, result.privateSupply ? "获得私人事件" : "抽取城市事件：" + result.name, result);
        if (result.privateSupply) { owner.contentNotice=result; owner.contentNoticeText="收到私人事件补给"; log(room,owner.name+" 收到一份私人补给"); }
        else { log(room,owner.name+" 触发「"+result.name+"」："+result.effect); if (result.targetId!=null) log(room,"本次排名目标已锁定为 "+player(room,result.targetId).name+"，并列时随机选择"); }
        GameEffect effect=effects.effect("CONTENT_EVENT",owner); effect.content=result; effects.submit(room,effect);
    }
    /** 输入已保存事件，展开确定的原子效果，选择只暂停后续步骤。 */
    private void resolveEvent(Room room, GameEffect source) {
        ContentResult r=source.content; Player owner=player(room,source.playerId); var p=r.parameters;
        switch (r.mechanism) {
            case "income" -> submit(room,"GRANT_MONEY",owner,p.get("amount"));
            case "fee" -> submit(room,"PAY_FEE",owner,p.get("amount"));
            case "forward","back" -> { submit(room,"MOVE",owner,p.get("steps")); submit(room,"SETTLE_TILE",owner,0); }
            case "skip" -> restrict(room,owner,p.get("count").intValue(),false);
            case "cards","cards_one","cards_two" -> { giveCards(room,owner,p.get("count").intValue(),null); overflow(room); }
            case "directed" -> { giveCards(room,owner,1,List.of("immunity","freeUpgrade","remote").get(p.get("cardType").intValue())); overflow(room); }
            case "all_income" -> { for (Player target:room.players) if (!target.bankrupt) submit(room,"GRANT_MONEY",target,p.get("amount")); }
            case "all_fee" -> submit(room,"CHARGE_ALL",owner,p.get("amount"));
            case "rich_fee" -> submit(room,"PAY_FEE",player(room,r.targetId),p.get("amount"));
            case "bottom_income" -> submit(room,"GRANT_MONEY",player(room,r.targetId),p.get("amount"));
            case "bottom_bonus" -> { submit(room,"GRANT_MONEY",owner,p.get("amount")); submit(room,"GRANT_MONEY",player(room,r.targetId),p.get("bonus")); }
            case "free_upgrade","gamble","self_destroy" -> { GameEffect e=effects.effect(switch(r.mechanism){case "free_upgrade"->"FREE_UPGRADE";case "gamble"->"GAMBLE";default->"SELF_DESTROY";},owner); e.content=r; effects.submit(room,e); }
            case "disaster" -> { GameEffect e=effects.effect("DESTROY_BUILDINGS",owner); e.selector="RANDOM_BUILT_PROPERTY"; effects.submit(room,e); }
            case "trap_immunity","next_waiver","rent_double" -> { GameEffect e=effects.effect("ADD_STATUS",owner); e.statusId=switch(r.mechanism){case "trap_immunity"->"TRAP_IMMUNITY";case "next_waiver"->"NEXT_PAYMENT_WAIVER";default->"NEXT_RENT_DOUBLE";}; e.amount=r.mechanism.equals("trap_immunity")?3:1; effects.submit(room,e); }
            default -> throw new GameError("内容机制不可用");
        }
    }
    /** 输入类型、作用人和已换算数值，追加具体效果，无返回值。 */
    private void submit(Room room,String type,Player owner,long amount) { GameEffect e=effects.effect(type,owner); e.amount=amount; effects.submit(room,e); }
    /** 输入作用人，返回其本回合尚未升级的未抵押、未满级地产。 */
    private static List<Integer> upgradeTargets(Room room, Player owner) { return room.board.stream().filter(t -> t.type.equals("property") && Objects.equals(t.ownerId,owner.id) && !t.mortgaged && t.level<Rules.MAX_LEVEL && !room.upgradedTiles.contains(t.id)).map(t -> t.id).toList(); }
    /** 输入免费升级事件，无目标才给固定替代现金，否则保存可放弃选择。 */
    private void freeUpgrade(Room room,GameEffect effect) {
        Player owner=player(room,effect.playerId); List<Integer> targets=upgradeTargets(room,owner);
        if (targets.isEmpty()) { submit(room,"GRANT_MONEY",owner,effect.content.parameters.get("fallback")); return; }
        ContentChoice choice=open(room,owner,"free_upgrade"); choice.tileIds.addAll(targets); choice.content=effect.content;
    }
    /** 输入自家毁楼事件，有楼时要求选地；无目标直接完成，不生成损失或补偿。 */
    private void selfDestroy(Room room,GameEffect effect) {
        Player owner=player(room,effect.playerId); List<Integer> targets=room.board.stream().filter(t -> Objects.equals(t.ownerId,owner.id) && t.level>0).map(t -> t.id).toList();
        if(targets.isEmpty()) { log(room,owner.name+" 没有可摧毁的自家建筑"); return; }
        ContentChoice choice=open(room,owner,"self_destroy"); choice.tileIds.addAll(targets); choice.content=effect.content;
    }
    /** 输入完整赌博参数，保存可参加或放弃窗口，不允许在该窗口抵押筹款。 */
    private void gamble(Room room,GameEffect effect) { ContentChoice choice=open(room,player(room,effect.playerId),"gamble"); choice.content=effect.content; }
    /** 输入刚施加的监禁或休息，解控持有人立即选择；无解控则直接施加对应限制。 */
    public void restrict(Room room,Player owner,int count,boolean jailed) {
        if (owner.hand.stream().anyMatch(c -> c.kind.equals("release"))) { ContentChoice choice=open(room,owner,"release"); choice.skipTurns=count; choice.jailed=jailed; return; }
        applyRestriction(room,owner,count,jailed);
    }
    /** 输入确定限制参数，保存后续跳过次数，不补走已跳过回合。 */
    private static void applyRestriction(Room room,Player owner,int count,boolean jailed) { owner.skipTurns=count; owner.jailed=jailed; log(room,owner.name+(jailed?" 进入监狱":" 需要休息")+"，跳过后续 "+count+" 次行动"); }
    /** 输入选择拥有者和类型，保存父事项并开放唯一私人选择，返回可填写参数的选择帧。 */
    private ContentChoice open(Room room,Player owner,String type) {
        require(room.runtime.choice==null && room.trade==null,"请先处理已有选择");
        ContentChoice choice=new ContentChoice(); choice.id=random.id(); choice.type=type; choice.playerId=owner.id;
        choice.returnPending=room.pending; choice.returnPhase=room.phase; room.runtime.choice=choice;
        room.pending=pending("choice",owner,null,0,null); room.pending.id=choice.id;
        room.runtime.effectsWaiting=true; return choice;
    }
    /** 输入当前私人选择及玩家答复，校验身份和全部参数后一次确认，恢复父效果位置。 */
    public void decide(Room room,Player owner,Action action) {
        ContentChoice choice=room.runtime.choice;
        require(choice!=null && choice.playerId.equals(owner.id) && choice.id.equals(action.decisionId),"此选择已失效，请同步最新局面");
        if(Set.of("free_upgrade","gamble","release").contains(choice.type)) require(action.accept!=null,"请选择确认或放弃");
        switch(choice.type) {
            case "hand_limit" -> {
                require(!Boolean.FALSE.equals(action.accept),"请确认保留六张道具");
                require(action.keepCardIds!=null && action.keepCardIds.size()==6 && new HashSet<>(action.keepCardIds).size()==6,"请恰好保留六张不同的道具");
                Set<String> kept=new HashSet<>(action.keepCardIds); require(choice.cards.stream().filter(c -> kept.contains(c.id)).count()==6 && owner.hand.size()==choice.cards.size(),"手牌候选已变化");
                owner.hand=new ArrayList<>(choice.cards.stream().filter(c -> kept.contains(c.id)).toList());
                owner.cash=Math.addExact(owner.cash,Math.multiplyExact(choice.cards.size()-6,choice.discardCash));
                log(room,owner.name+" 已整理私人补给");
            }
            case "free_upgrade" -> {
                if (Boolean.TRUE.equals(action.accept)) {
                    require(choice.tileIds.contains(action.tileId) && upgradeTargets(room,owner).contains(action.tileId),"请选择合法的自家地产");
                    Tile target=tile(room,action.tileId);
                    if(choice.cardId!=null) consume(room,owner,find(owner,choice.cardId),true);
                    target.level++; room.upgradedTiles.add(target.id); log(room,owner.name+" 将 "+target.name+" 免费升至 "+target.level+" 级");
                }
            }
            case "self_destroy" -> {
                require(!Boolean.FALSE.equals(action.accept),"请选择目标并确认摧毁建筑");
                require(choice.tileIds.contains(action.tileId),"请选择自己的有建筑地产"); Tile target=tile(room,action.tileId);
                require(target!=null && Objects.equals(target.ownerId,owner.id) && target.level>0,"该建筑已不可用");
                GameEffect e=effects.effect("DESTROY_BUILDINGS",owner); e.tileId=target.id; e.resumesDecision=true; effects.submit(room,e);
            }
            case "gamble" -> {
                if (Boolean.TRUE.equals(action.accept)) {
                    long wager=choice.content.parameters.get("wager"); require(owner.cash>=wager,"赌博仅允许使用已有现金"); owner.cash-=wager;
                    choice.sample=random.nextInt(100); choice.content.sample=choice.sample;
                    if(room.runtime.lastContent!=null && room.runtime.lastContent.id.equals(choice.content.id)) room.runtime.lastContent.sample=choice.sample;
                    boolean won=choice.sample<choice.content.parameters.get("probability");
                    if(won) owner.cash=Math.addExact(owner.cash,choice.content.parameters.get("prize"));
                    log(room,owner.name+" 下注 ¥"+wager+(won?"，中奖获得 ¥"+choice.content.parameters.get("prize"):"，本次未中奖"));
                } else log(room,owner.name+" 放弃本次赌博");
            }
            case "release" -> {
                if(Boolean.TRUE.equals(action.accept)) {
                    Card card=find(owner,action.cardId); require(card.kind.equals("release"),"请选择解控卡"); consume(room,owner,card,false);
                    log(room,owner.name+" 解除本次"+(choice.jailed?"监禁":"休息"));
                } else applyRestriction(room,owner,choice.skipTurns,choice.jailed);
            }
            default -> throw new GameError("选择类型不可用");
        }
        room.pending=choice.returnPending; room.phase=choice.returnPhase; room.runtime.choice=null;
    }
    /** 输入真实主动卡，先验证完整目标；免费升级延迟消费，无牌抢夺保留卡和次数。 */
    public void use(Room room,Player owner,Action action) {
        Card card=find(owner,action.cardId); require(card.skinId!=null,"请使用兼容道具路径");
        require(room.phase.equals("pre_roll") && room.pending==null && !room.activeCardUsed && !Set.of("immunity","halfPayment","release").contains(card.kind),"每回合掷骰前最多主动使用一张道具");
        Tile target=tile(room,action.tileId); Player opponent=player(room,action.playerId);
        switch(card.kind) {
            case "fixed","move","remote" -> {
                long steps=card.kind.equals("remote") ? (action.point==null?0:action.point) : card.parameters.get("steps");
                require(!card.kind.equals("remote") || steps>=1 && steps<=6,"请选择 1～6 点"); consume(room,owner,card,true);
                submit(room,"REPLACE_ROLL",owner,steps);
            }
            case "barrier" -> {
                int distance=target==null?0:Math.floorMod(target.id-owner.position,room.board.size());
                require(target!=null && target.type.equals("property") && !target.barrier && distance>=1 && distance<=6,"请选择前方六格内没有路障的地产"); consume(room,owner,card,true);
                GameEffect e=effects.effect("PLACE_BARRIER",owner); e.tileId=target.id; effects.submit(room,e);
            }
            case "demolish" -> {
                require(target!=null && target.ownerId!=null && !target.ownerId.equals(owner.id) && target.level>0,"请选择对手有建筑的地产"); consume(room,owner,card,true);
                GameEffect e=effects.effect("DEMOLISH",owner); e.tileId=target.id; effects.submit(room,e);
            }
            case "swap" -> {
                require(opponent!=null && !opponent.bankrupt && !opponent.id.equals(owner.id) && !opponent.jailed,"请选择未服刑的对手"); consume(room,owner,card,true);
                GameEffect e=effects.effect("SWAP_POSITION",owner); e.targetId=opponent.id; effects.submit(room,e);
            }
            case "bonus" -> { consume(room,owner,card,true); room.runtime.rollBonus=card.parameters.get("steps").intValue(); log(room,owner.name+" 为本次掷骰增加 "+room.runtime.rollBonus+" 格"); }
            case "cash" -> { consume(room,owner,card,true); submit(room,"GRANT_MONEY",owner,card.parameters.get("amount")); }
            case "freeUpgrade" -> {
                List<Integer> targets=upgradeTargets(room,owner); if(targets.isEmpty()) { owner.contentNotice=card; owner.contentNoticeText="没有合法免费升级目标，此卡已保留"; return; }
                ContentChoice choice=open(room,owner,"free_upgrade"); choice.tileIds.addAll(targets); choice.cardId=card.id; choice.content=card;
            }
            case "steal" -> {
                require(opponent!=null && !opponent.bankrupt && !opponent.id.equals(owner.id),"请选择未破产的对手");
                if(opponent.hand.isEmpty()) { owner.contentNotice=card; owner.contentNoticeText="对手没有手牌，抢夺卡和主动次数已保留"; return; }
                Card transferred=opponent.hand.get(random.nextInt(opponent.hand.size())); consume(room,owner,card,true); opponent.hand.remove(transferred); owner.hand.add(transferred);
                owner.contentNotice=transferred; owner.contentNoticeText="抢夺获得这张道具"; opponent.contentNotice=transferred; opponent.contentNoticeText="这张道具被转移给 "+owner.name;
                log(room,owner.name+" 对 "+opponent.name+" 发动私人道具转移"); overflow(room);
            }
            default -> throw new GameError("该道具不可主动使用");
        }
    }
    /** 输入强制费用和防御答复，仅本次首个选择可减免；先于任何资产抵债。 */
    public void defense(Room room,Player owner,Action action) {
        require(paymentPending(room) && room.pending.playerId.equals(owner.id) && !room.pending.defenseResolved,"本笔费用已完成防御选择");
        PendingDecision debt=room.pending; require(action.decisionId==null || Objects.equals(action.decisionId,debt.id),"费用选择已失效");
        if(action.cardId!=null) {
            Card card=find(owner,action.cardId);
            require(card.kind.equals("halfPayment") || card.kind.equals("immunity") && debt.type.equals("rent"),"此道具不能减免本笔费用");
            consume(room,owner,card,false); long reduction=card.kind.equals("immunity")?debt.amount:debt.amount/2; debt.amount-=reduction;
            log(room,owner.name+" 本笔费用减免 ¥"+reduction+"，剩余 ¥"+debt.amount);
        }
        debt.defenseResolved=true;
        if(debt.amount==0) { room.pending=null; room.phase="manage"; }
    }
    /** 输入手牌身份，返回仅属于调用者的真实卡牌。 */
    private static Card find(Player owner,String id) { return owner.hand.stream().filter(c -> Objects.equals(c.id,id)).findFirst().orElseThrow(() -> new GameError("该道具已不在你的手牌中")); }
    /** 输入已验证的卡牌，消费并登记主动次数，不公开卡面或剩余手牌。 */
    private static void consume(Room room,Player owner,Card card,boolean active) { owner.hand.remove(card); if(active) room.activeCardUsed=true; }
}
