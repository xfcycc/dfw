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
    private final ExpansionService expansion;
    /** 动漫扩展使用同一选择及效果队列。 */
    public final AnimeService anime;
    /** 八素材扩展沿用同一效果与选择链路。 */
    public final NetaService neta;
    /** 素材扩展共用原攻击响应、效果队列和私人选择。 */
    public final CrossoverService crossover;
    /** 输入已校验目录及结算器，注册需要选择的内容效果。 */
    public ContentService(ContentCatalog catalog, EffectResolver effects, RandomSource random) {
        this.catalog = catalog; this.effects = effects; this.random = random;
        this.expansion = new ExpansionService(this, effects, random, catalog);
        this.anime = new AnimeService(this, effects, random);
        this.crossover = new CrossoverService(this,effects,catalog,random);
        this.neta = new NetaService(this, effects, random, catalog);
        effects.beforeEffect(this::offerMovementRelease);
        effects.register("CONTENT_EVENT", this::resolveEvent);
        effects.register("CONSTRUCTION_SHRINE", (room,effect) -> ConstructionService.shrine(room,effect));
        effects.register("FREE_UPGRADE", this::freeUpgrade);
        effects.register("FULL_UPGRADE", this::freeUpgrade);
        effects.register("SELF_DESTROY", this::selfDestroy);
        effects.register("OPPONENT_DESTROY", this::selfDestroy);
        effects.register("GAMBLE", this::gamble);
        effects.register("GRANT_RANDOM_LAND", this::grantLand);
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
    static Card card(ContentResult r) {
        Card c = new Card(); c.id=r.id; c.pool=r.pool; c.mechanism=r.mechanism; c.parameterId=r.parameterId; c.skinId=r.skinId;
        c.name=r.name; c.rarity=r.rarity; c.quote=r.quote; c.description=r.description; c.cardType=r.cardType; c.effect=r.effect; c.parameters=new LinkedHashMap<>(r.parameters); c.kind=r.mechanism; return c;
    }
    /** 输入房间，查找超过上限的旧存档或新补给；保存父费用等待，不覆盖交易或另一选择。 */
    public boolean overflow(Room room) {
        if (room.trade != null || room.runtime.choice != null || !room.status.equals("playing")) return false;
        Player owner = room.players.stream().filter(p -> !p.bankrupt && p.hand.size() > PropertyEffects.handLimit(p)).findFirst().orElse(null);
        if (owner == null) return false;
        ContentChoice choice = open(room,owner,"hand_limit"); choice.cards.addAll(owner.hand);
        choice.discardCash = Rules.economy(room).money("handDiscard",200); return true;
    }
    /** 输入房间，返回是否需继续整理超限手牌，连接恢复时交给原流程推进。 */
    public boolean needsOverflow(Room room) { return room.trade == null && room.runtime.choice == null && room.players.stream().anyMatch(p -> !p.bankrupt && p.hand.size() > PropertyEffects.handLimit(p)); }
    /** 输入房间、落格玩家和池，抽取并保存事件及关联路径，锁定排名目标后入队，无返回值。 */
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
        if (!result.privateSupply && !room.movementSequence.isEmpty()) {
            MotionSegment segment = room.movementSequence.getLast();
            if (segment.playerId.equals(owner.id)) segment.event = result;
        }
        HistoryTrace.event(room, "event_drawn", owner.id, result.privateSupply ? "获得私人事件" : "抽取城市事件：" + result.name, result);
        if (result.privateSupply) { owner.contentNotice=result; owner.contentNoticeText="收到私人事件补给"; log(room,owner.name+" 收到一份私人补给"); }
        else { contentLog(room,"log",owner.id,owner.name+" 触发「"+result.name+"」："+result.effect,result); if (result.targetId!=null) log(room,"本次排名目标已锁定为 "+player(room,result.targetId).name+"，并列时随机选择"); }
        GameEffect effect=effects.effect("CONTENT_EVENT",owner); effect.content=result; effects.submit(room,effect);
    }
    /** 输入房间和已保存事件效果，展开原子效果并保存移动来源；入狱事件直接作用于抽到者，无返回值。 */
    private void resolveEvent(Room room, GameEffect source) {
        ContentResult r=source.content; Player owner=player(room,source.playerId); var p=r.parameters;
        if(crossover.event(room,owner,r)) return;
        switch (r.mechanism) {
            case "allConstruction", "erdtreeBlessing" -> ConstructionService.event(this,room,owner,r,random);
            case "classTrial" -> anime.trial(room,owner,r);
            case "igniteSea","geniusClub","senbonzakura" -> neta.event(room,owner,r);
            case "income" -> submit(room,"GRANT_MONEY",owner,p.get("amount"));
            case "fee" -> submitFee(room,"PAY_FEE",owner,p.get("amount"),r);
            case "forward","back" -> {
                GameEffect movement = effects.effect("MOVE", owner); movement.amount = p.get("steps"); movement.content = r;
                effects.submit(room, movement); submit(room,"SETTLE_TILE",owner,0);
            }
            case "skip" -> restrict(room,owner,p.get("count").intValue(),false);
            case "jail" -> effects.submit(room,effects.effect("SEND_TO_JAIL",owner));
            case "cards","cards_one","cards_two" -> { giveCards(room,owner,p.get("count").intValue(),null); overflow(room); }
            case "directed" -> { giveCards(room,owner,1,List.of("immunity","freeUpgrade","remote").get(p.get("cardType").intValue())); overflow(room); }
            case "all_income" -> { for (Player target:room.players) if (!target.bankrupt) submit(room,"GRANT_MONEY",target,p.get("amount")); }
            case "all_fee" -> submitFee(room,"CHARGE_ALL",owner,p.get("amount"),r);
            case "rich_fee" -> submitFee(room,"PAY_FEE",player(room,r.targetId),p.get("amount"),r);
            case "bottom_income" -> submit(room,"GRANT_MONEY",player(room,r.targetId),p.get("amount"));
            case "bottom_bonus" -> { submit(room,"GRANT_MONEY",owner,p.get("amount")); submit(room,"GRANT_MONEY",player(room,r.targetId),p.get("bonus")); }
            case "free_upgrade","full_upgrade","gamble","self_destroy","opponent_destroy" -> { GameEffect e=effects.effect(switch(r.mechanism){case "free_upgrade"->"FREE_UPGRADE";case "full_upgrade"->"FULL_UPGRADE";case "gamble"->"GAMBLE";case "opponent_destroy"->"OPPONENT_DESTROY";default->"SELF_DESTROY";},owner); e.content=r; effects.submit(room,e); }
            case "disaster" -> { GameEffect e=effects.effect("DESTROY_BUILDINGS",owner); e.selector="RANDOM_BUILT_PROPERTY"; effects.submit(room,e); }
            case "all_morale" -> {
                for (Player target : room.players) if (!target.bankrupt) { GameEffect e=effects.effect("ADD_STATUS",target); e.statusId="NEXT_MOVE_DOUBLE"; e.amount=1; effects.submit(room,e); }
            }
            case "talent" -> { submit(room,"GRANT_RANDOM_LAND",owner,0); submit(room,"GRANT_MONEY",owner,p.get("amount")); submit(room,"DRAW_CARDS",owner,p.get("count")); }
            case "global_demolition" -> { GameEffect e=effects.effect("LOWER_ALL_BUILDINGS",owner); e.content=r; e.compensationPercent=p.get("compensationPercent").intValue(); effects.submit(room,e); }
            case "trap_immunity","next_waiver","rent_double" -> { GameEffect e=effects.effect("ADD_STATUS",owner); e.statusId=switch(r.mechanism){case "trap_immunity"->"TRAP_IMMUNITY";case "next_waiver"->"NEXT_PAYMENT_WAIVER";default->"NEXT_RENT_DOUBLE";}; e.amount=r.mechanism.equals("trap_immunity")?3:1; effects.submit(room,e); }
            default -> expansion.event(room, owner, r);
        }
    }
    /** 输入类型、作用人和已换算数值，追加具体效果，无返回值。 */
    private void submit(Room room,String type,Player owner,long amount) { GameEffect e=effects.effect(type,owner); e.amount=amount; effects.submit(room,e); }
    /** 输入费用类型、付款人、金额和真实事件来源，入队时保留来源供清算结算展示。 */
    private void submitFee(Room room, String type, Player owner, long amount, ContentResult source) {
        GameEffect e = effects.effect(type, owner); e.amount = amount; e.content = source; effects.submit(room, e);
    }
    /** 输入人才引进领地效果，随机授予一块无主地产；无目标只跳过领地，不阻断后续现金与补给。 */
    private void grantLand(Room room, GameEffect effect) {
        Player owner=player(room,effect.playerId);
        List<Tile> targets=room.board.stream().filter(t -> t.type.equals("property") && t.ownerId==null && !PropertyEffects.frozen(t)).toList();
        if(targets.isEmpty()) { log(room,"人才引进：当前没有无主地产，生活费和道具照常发放"); return; }
        Tile target=targets.get(random.nextInt(targets.size())); target.ownerId=owner.id;
        log(room,"log",owner.id,owner.name+" 通过人才引进免费获得 "+target.name+"（"+target.id+"号）");
        HistoryTrace.event(room,"land_granted",owner.id,"人才引进地产已确定",target);
    }
    /** 输入作用人，返回其本回合尚未升级的未抵押、未满级地产。 */
    private static List<Integer> upgradeTargets(Room room, Player owner) { return room.board.stream().filter(t -> t.type.equals("property") && !PropertyEffects.frozen(t) && Objects.equals(t.ownerId,owner.id) && !t.mortgaged && !AnimeService.iced(t) && t.level<Rules.MAX_LEVEL && !room.upgradedTiles.contains(t.id)).map(t -> t.id).toList(); }
    /** 输入普通或满级升级事件，无目标才给固定替代现金，否则保存可放弃的合法地产选择。 */
    private void freeUpgrade(Room room,GameEffect effect) {
        Player owner=player(room,effect.playerId); List<Integer> targets=upgradeTargets(room,owner);
        if (targets.isEmpty()) { submit(room,"GRANT_MONEY",owner,effect.content.parameters.get("fallback")); return; }
        ContentChoice choice=open(room,owner,effect.content.mechanism.equals("full_upgrade") ? "full_upgrade" : "free_upgrade"); choice.tileIds.addAll(targets); choice.content=effect.content;
    }
    /** 输入房间、选择者及是否攻击对手，返回对应未破产业主的有建筑地产。 */
    private static List<Integer> destroyTargets(Room room, Player owner, boolean opponent) {
        Set<String> owners = new HashSet<>();
        for (Player candidate : room.players) if (!candidate.bankrupt && (opponent ? !candidate.id.equals(owner.id) : candidate.id.equals(owner.id))) owners.add(candidate.id);
        return room.board.stream().filter(t -> t.type.equals("property") && !PropertyEffects.frozen(t) && t.level>0 && owners.contains(t.ownerId)).map(t -> t.id).toList();
    }
    /** 输入房间及已保存选择，返回仍满足产权、建筑和本回合升级限制的原候选交集。 */
    public static List<Integer> choiceTargets(Room room, ContentChoice choice) {
        Player owner = player(room, choice.playerId);
        List<Integer> current = switch (choice.type) {
            case "free_upgrade", "full_upgrade" -> upgradeTargets(room, owner);
            case "self_destroy", "opponent_destroy" -> destroyTargets(room, owner, choice.type.equals("opponent_destroy"));
            default -> choice.tileIds;
        };
        return choice.tileIds.stream().filter(current::contains).toList();
    }
    /** 输入自家或对手全毁事件，有楼时保存选地窗口；无目标直接完成，不生成损失或补偿。 */
    private void selfDestroy(Room room,GameEffect effect) {
        Player owner=player(room,effect.playerId); boolean opponent=effect.content.mechanism.equals("opponent_destroy"); List<Integer> targets=destroyTargets(room,owner,opponent);
        if(targets.isEmpty()) { log(room,owner.name+(opponent ? " 没有可摧毁的对手建筑" : " 没有可摧毁的自家建筑")); return; }
        ContentChoice choice=open(room,owner,opponent ? "opponent_destroy" : "self_destroy"); choice.tileIds.addAll(targets); choice.content=effect.content;
    }
    /** 输入完整赌博参数，保存可参加或放弃窗口，不允许在该窗口抵押筹款。 */
    private void gamble(Room room,GameEffect effect) { ContentChoice choice=open(room,player(room,effect.playerId),"gamble"); choice.content=effect.content; }
    /** 输入队首效果，在外部位移前开放一次解控响应；换位双方在任何一方移动前共同确认。 */
    private boolean offerMovementRelease(Room room,GameEffect effect) {
        if(effect.movementCancelled) return false;
        List<GameEffect> movements=List.of(effect);
        List<String> targets;
        switch(effect.type) {
            case "ANIME_PART" -> {
                if(effect.propertyTarget || !Set.of("teleport","gather","blast","command").contains(effect.selector)) return false;
                if(effect.content!=null && Set.of("swap","swapAny").contains(effect.content.mechanism)) {
                    movements=room.runtime.effects.stream().filter(e -> e.type.equals("ANIME_PART") && "teleport".equals(e.selector) && e.content!=null && Objects.equals(e.content.id,effect.content.id)).toList();
                }
                targets=movements.stream().filter(e -> !Objects.equals(e.playerId,e.targetId)).map(e -> e.targetId).distinct().toList();
            }
            case "SWAP_POSITION", "RANDOM_TELEPORT" -> targets=Objects.equals(effect.playerId,effect.targetId)?List.of():List.of(effect.targetId);
            case "SEND_TO_JAIL" -> {
                Player target=player(room,effect.playerId);
                if(effect.voluntaryRestriction || target==null || StatusService.controlProtectionTurns(target)>0 || room.board.get(target.position).type.equals("jail")) return false;
                targets=List.of(target.id);
            }
            case "MOVE" -> {
                if(effect.amount==0 || effect.content==null || !Set.of("forward","back").contains(effect.content.mechanism)) return false;
                targets=List.of(effect.playerId);
            }
            case "EVENT_RELOCATE", "PORTAL_TRANSFER" -> targets=List.of(effect.playerId);
            default -> { return false; }
        }
        for(String id:targets) {
            Player owner=player(room,id);
            if(owner==null || owner.bankrupt || effect.movementReleasePlayers.contains(id)) continue;
            List<Card> cards=owner.hand.stream().filter(c -> c.kind.equals("release") && CrossoverService.handAllowed(room,owner,c,false)).toList();
            if(cards.isEmpty()) continue;
            ContentChoice choice=open(room,owner,"movement_release"); choice.cards.addAll(cards); choice.content=effect.content; choice.jailed=effect.type.equals("SEND_TO_JAIL");
            choice.effectIds.addAll(movements.stream().map(e -> e.id).toList());
            // 事件推动与随后落点是一组，抵消后不能在原格重复结算。
            if(effect.type.equals("MOVE") && room.runtime.effects.size()>1) {
                GameEffect landing=room.runtime.effects.get(1);
                if(landing.type.equals("SETTLE_TILE") && Objects.equals(landing.playerId,owner.id)) choice.effectIds.add(landing.id);
            }
            choice.prompt=choice.jailed?"即将被传送入狱，可消耗1张解控卡，同时抵消本次传送和监禁。":movements.size()>1 || effect.type.equals("SWAP_POSITION")?"即将被交换位置，可消耗1张解控卡抵消整次换位，双方留在原位。":"即将受到位移效果，可消耗1张解控卡抵消本次位移；其他玩家及其他效果照常结算。";
            return true;
        }
        return false;
    }
    /** 输入已保存位移响应及答复；确认消费一张解控卡并移除关联效果，放弃只记录答复。 */
    private void decideMovementRelease(Room room,Player owner,ContentChoice choice,Action action) {
        require(action.accept!=null,"请选择抵消或接受本次位移");
        require(room.runtime.effects.stream().anyMatch(e -> choice.effectIds.contains(e.id)),"此位移效果已失效");
        if(action.accept) {
            Card card=find(owner,action.cardId); require(card.kind.equals("release") && choice.cards.stream().anyMatch(c -> c.id.equals(card.id)),"请选择本次可用的解控卡");
            consume(room,owner,card,false,action);
            room.runtime.effects.removeIf(e -> {
                if(!choice.effectIds.contains(e.id)) return false;
                if(e.type.equals("PORTAL_TRANSFER")) { e.movementCancelled=true; return false; }
                return true;
            });
            log(room,owner.name+" 使用解控卡抵消本次位移"+(choice.jailed?"及监禁":"")); BoardEffect.cue(room,"release",owner);
        } else for(GameEffect e:room.runtime.effects) if(choice.effectIds.contains(e.id)) e.movementReleasePlayers.add(owner.id);
        room.pending=choice.returnPending; room.phase=choice.returnPhase; room.runtime.choice=null;
    }
    /** 输入刚施加的监禁或休息，解控持有人立即选择；无解控则直接施加对应限制。 */
    public void restrict(Room room,Player owner,int count,boolean jailed) {
        restrict(room,owner,count,jailed,true);
    }
    /** 输入限制参数及是否为外部控制；保护阻挡外部休息和监禁，自愿休息代价仍须承担。 */
    public void restrict(Room room,Player owner,int count,boolean jailed,boolean controlled) {
        if(controlled && StatusService.controlProtectionTurns(owner)>0) { log(room,owner.name+" 的连续控制保护抵挡了本次"+(jailed?"监禁":"休息")); return; }
        if (owner.hand.stream().anyMatch(c -> c.kind.equals("release") && CrossoverService.handAllowed(room,owner,c,false))) { ContentChoice choice=open(room,owner,"release"); choice.skipTurns=count; choice.jailed=jailed; choice.voluntaryRestriction=!controlled; BoardEffect.cue(room,jailed?"jail":"rest",owner); return; }
        applyRestriction(room,owner,count,jailed,controlled);
    }
    /** 输入确定限制及外部控制标记，保存跳过次数并登记保护；监禁立即停租，普通休息仍可收租，无返回值。 */
    private static void applyRestriction(Room room,Player owner,int count,boolean jailed,boolean controlled) { SettlementService.observe(room); if (jailed && count > 0) owner.matchStats.jailCount++; owner.skipTurns=Math.max(owner.skipTurns,count); if(!controlled) owner.voluntarySkipTurns=Math.max(owner.voluntarySkipTurns,count); owner.jailed=jailed || owner.jailed; if(controlled && count>0) StatusService.protectControl(room,owner); log(room,owner.name+(jailed?" 进入监狱":" 需要休息")+"，跳过后续 "+count+" 次行动"+(jailed?"；服刑期间暂停收租，其他小游戏正常参与":"")); BoardEffect.cue(room,jailed?"jail":"rest",owner); }
    /** 输入选择拥有者和类型，保存父事项并开放唯一私人选择，返回可填写参数的选择帧。 */
    ContentChoice open(Room room,Player owner,String type) {
        require(room.runtime.choice==null && room.trade==null,"请先处理已有选择");
        ContentChoice choice=new ContentChoice(); choice.id=random.id(); choice.type=type; choice.playerId=owner.id;
        choice.returnPending=room.pending; choice.returnPhase=room.phase; room.runtime.choice=choice;
        room.pending=pending("choice",owner,null,0,null); room.pending.id=choice.id;
        room.runtime.effectsWaiting=true; return choice;
    }
    /** 输入选择身份及答复，复核目标并恢复父位置；摧毁候选全部失效时按无目标规则结束，不产生损失补偿。 */
    public void decide(Room room,Player owner,Action action) {
        ContentChoice choice=room.runtime.choice;
        require(choice!=null && choice.playerId.equals(owner.id) && choice.id.equals(action.decisionId),"此选择已失效，请同步最新局面");
        if(choice.type.equals("movement_release")) { decideMovementRelease(room,owner,choice,action); return; }
        if(choice.type.startsWith("construction_")) { ConstructionService.decide(this,room,owner,choice,action); return; }
        if(neta.decide(room,owner,choice,action)) return;
        if(crossover.decide(room,owner,choice,action)) return;
        if(anime.decide(room,owner,choice,action)) return;
        if(Set.of("free_upgrade","full_upgrade","gamble","release").contains(choice.type)) require(action.accept!=null,"请选择确认或放弃");
        switch(choice.type) {
            case "recycle", "discover" -> expansion.decide(room, owner, choice, action);
            case "hand_limit" -> {
                int limit = PropertyEffects.handLimit(owner);
                require(!Boolean.FALSE.equals(action.accept),"请确认保留上限数量的道具");
                require(action.keepCardIds!=null && action.keepCardIds.size()==limit && new HashSet<>(action.keepCardIds).size()==limit,"请按当前上限保留不同的道具");
                Set<String> kept=new HashSet<>(action.keepCardIds); require(choice.cards.stream().filter(c -> kept.contains(c.id)).count()==limit && owner.hand.size()==choice.cards.size(),"手牌候选已变化");
                owner.hand=new ArrayList<>(choice.cards.stream().filter(c -> kept.contains(c.id)).toList());
                owner.cash=Math.addExact(owner.cash,Math.multiplyExact(choice.cards.size()-limit,choice.discardCash));
                log(room,owner.name+" 已整理私人补给");
                if (choice.cards.size()>limit && choice.discardCash>0) BoardEffect.cue(room,"income",owner);
            }
            case "free_upgrade","full_upgrade" -> {
                if (Boolean.TRUE.equals(action.accept)) {
                    require(choice.tileIds.contains(action.tileId) && upgradeTargets(room,owner).contains(action.tileId),"请选择合法的自家地产");
                    Tile target=tile(room,action.tileId);
                    BoardEffect.Observation presentation=BoardEffect.watch(room,"upgrade",owner);
                    if(choice.cardId!=null) consume(room,owner,find(owner,choice.cardId),true,action);
                    int previousLevel=target.level; target.level=choice.type.equals("full_upgrade") ? Rules.MAX_LEVEL : target.level+1;
                    SettlementService.observe(room); owner.matchStats.builtLevels += target.level - previousLevel;
                    room.upgradedTiles.add(target.id); log(room,"log",owner.id,owner.name+" 免费升级 "+target.name+"（"+target.id+"号），建筑从 "+previousLevel+" 级升至 "+target.level+" 级，费用 ¥0，现金余额 ¥"+String.format("%,d",owner.cash));
                    presentation.finish(room);
                }
            }
            case "self_destroy","opponent_destroy" -> {
                require(!Boolean.FALSE.equals(action.accept),"请选择目标并确认摧毁建筑");
                List<Integer> targets = choiceTargets(room, choice);
                if (targets.isEmpty()) log(room, owner.name + " 的摧毁选择已无合法建筑目标，本次效果结束");
                else {
                    require(targets.contains(action.tileId),"请选择合法的有建筑地产"); Tile target=tile(room,action.tileId);
                    GameEffect e=effects.effect("DESTROY_BUILDINGS",owner); e.tileId=target.id; e.resumesDecision=true; effects.submit(room,e);
                }
            }
            case "gamble" -> {
                if (Boolean.TRUE.equals(action.accept)) {
                    long wager=choice.content.parameters.get("wager"); require(owner.cash>=wager,"赌博仅允许使用已有现金");
                    if (choice.cardId != null) consume(room,owner,find(owner,choice.cardId),true,action);
                    owner.cash-=wager;
                    choice.sample=random.nextInt(100); choice.content.sample=choice.sample;
                    if (choice.cardId != null) room.runtime.playedCard.sample=choice.sample;
                    if(room.runtime.lastContent!=null && room.runtime.lastContent.id.equals(choice.content.id)) room.runtime.lastContent.sample=choice.sample;
                    boolean won=choice.sample<choice.content.parameters.get("probability");
                    if(won) owner.cash=Math.addExact(owner.cash,choice.content.parameters.get("prize"));
                    log(room,owner.name+" 下注 ¥"+wager+(won?"，中奖获得 ¥"+choice.content.parameters.get("prize"):"，本次未中奖"));
                    BoardEffect.cue(room,won?"gambleWin":"gambleLose",owner);
                } else if (choice.cardId == null) log(room,owner.name+" 放弃本次赌博");
            }
            case "release" -> {
                if(Boolean.TRUE.equals(action.accept)) {
                    Card card=find(owner,action.cardId); require(card.kind.equals("release"),"请选择解控卡"); consume(room,owner,card,false,action);
                    log(room,owner.name+" 解除本次"+(choice.jailed?"监禁":"休息"));
                    BoardEffect.cue(room,"release",owner);
                } else applyRestriction(room,owner,choice.skipTurns,choice.jailed,!choice.voluntaryRestriction);
            }
            default -> throw new GameError("选择类型不可用");
        }
        room.pending=choice.returnPending; room.phase=choice.returnPhase; room.runtime.choice=null;
    }
    /** 输入房间、持牌者及主动卡操作，校验附近路障地块或其他目标，确认后消费，无返回值。 */
    public void use(Room room,Player owner,Action action) {
        Card card=find(owner,action.cardId); require(card.skinId!=null || AnimeService.attacks(card.kind),"请使用兼容道具路径");
        require(room.phase.equals("pre_roll") && room.pending==null && AnimeService.usable(room,card) && !AnimeService.DEFENSE.contains(card.kind) && !Set.of("immunity","halfPayment","release").contains(card.kind),"每回合掷骰前最多主动使用一张道具");
        CrossoverService.validateActive(room,owner,card);
        if(crossover.use(room,owner,card,action)) return;
        if(ConstructionService.CARDS.contains(card.kind)) { ConstructionService.use(this,room,owner,card); return; }
        if(neta.use(room,owner,card,action)) return;
        if(anime.use(room,owner,card,action)) return;
        Tile target=tile(room,action.tileId); Player opponent=player(room,action.playerId);
        switch(card.kind) {
            case "gamble" -> {
                ContentChoice choice=open(room,owner,"gamble"); choice.cardId=card.id; choice.content=card;
            }
            case "fixed","move","remote" -> {
                long steps=card.kind.equals("remote") ? (action.point==null?0:action.point) : card.parameters.get("steps");
                require(!card.kind.equals("remote") || steps>=1 && steps<=6,"请选择 1～6 点"); consume(room,owner,card,true,action);
                submit(room,"REPLACE_ROLL",owner,steps);
            }
            case "barrier" -> {
                int distance=target==null?0:nearbyDistance(owner.position,target.id,room.board.size());
                require(target!=null && !PropertyEffects.frozen(target) && !target.barrier && distance>=1 && distance<=6,"请选择附近 6 格内没有路障的地块（前后各 6 格，不含当前格）"); consume(room,owner,card,true,action);
                GameEffect e=effects.effect("PLACE_BARRIER",owner); e.tileId=target.id; effects.submit(room,e);
            }
            case "demolish","fullDemolish" -> {
                require(target!=null && destroyTargets(room,owner,true).contains(target.id),"请选择对手有建筑的地产"); consume(room,owner,card,true,action);
                GameEffect e=effects.effect(card.kind.equals("fullDemolish") ? "DESTROY_BUILDINGS" : "DEMOLISH",owner); e.tileId=target.id; e.content=card; effects.submit(room,e);
            }
            case "swap" -> {
                require(opponent!=null && !opponent.bankrupt && !opponent.id.equals(owner.id) && !opponent.jailed,"请选择未服刑的对手"); consume(room,owner,card,true,action);
                GameEffect e=effects.effect("SWAP_POSITION",owner); e.targetId=opponent.id; effects.submit(room,e);
            }
            case "blast" -> {
                require(opponent!=null && !opponent.bankrupt,"请选择一名未破产玩家，可选择自己"); consume(room,owner,card,true,action);
                GameEffect e=effects.effect("RANDOM_TELEPORT",owner); e.targetId=opponent.id; effects.submit(room,e);
            }
            case "jail" -> {
                require(opponent!=null && !opponent.bankrupt,"请选择一名未破产玩家，可选择自己"); consume(room,owner,card,true,action);
                GameEffect jail=effects.effect("SEND_TO_JAIL",opponent); jail.voluntaryRestriction=opponent==owner; effects.submit(room,jail);
            }
            case "bonus" -> { consume(room,owner,card,true,action); room.runtime.rollBonus=card.parameters.get("steps").intValue(); log(room,owner.name+" 为本次掷骰增加 "+room.runtime.rollBonus+" 格"); BoardEffect.cue(room,"moveBoost",owner); }
            case "cash" -> { consume(room,owner,card,true,action); submit(room,"GRANT_MONEY",owner,card.parameters.get("amount")); }
            case "freeUpgrade","fullUpgrade" -> {
                List<Integer> targets=upgradeTargets(room,owner); if(targets.isEmpty()) { owner.contentNotice=card; owner.contentNoticeText="没有合法免费升级目标，此卡已保留"; return; }
                ContentChoice choice=open(room,owner,card.kind.equals("fullUpgrade") ? "full_upgrade" : "free_upgrade"); choice.tileIds.addAll(targets); choice.cardId=card.id; choice.content=card;
            }
            case "steal" -> {
                require(opponent!=null && !opponent.bankrupt && !opponent.id.equals(owner.id),"请选择未破产的对手");
                if(opponent.hand.isEmpty()) { owner.contentNotice=card; owner.contentNoticeText="对手没有手牌，抢夺卡和主动次数已保留"; return; }
                Card transferred=opponent.hand.get(random.nextInt(opponent.hand.size())); consume(room,owner,card,true,action); opponent.hand.remove(transferred); owner.hand.add(transferred);
                owner.contentNotice=transferred; owner.contentNoticeText="抢夺获得这张道具"; opponent.contentNotice=transferred; opponent.contentNoticeText="这张道具被转移给 "+owner.name;
                log(room,owner.name+" 对 "+opponent.name+" 发动私人道具转移"); overflow(room);
                BoardEffect.cue(room,"steal",owner,opponent);
            }
            default -> expansion.use(room, owner, card, action);
        }
    }
    /** 输入强制费用和防御答复，仅本次首个选择可减免；先于任何资产抵债。 */
    public void defense(Room room,Player owner,Action action) {
        require(paymentPending(room) && room.pending.playerId.equals(owner.id) && !room.pending.defenseResolved,"本笔费用已完成防御选择");
        PendingDecision debt=room.pending; require(action.decisionId==null || Objects.equals(action.decisionId,debt.id),"费用选择已失效");
        if(action.cardId!=null) {
            Card card=find(owner,action.cardId);
            require(card.kind.equals("halfPayment") || card.kind.equals("immunity") && debt.type.equals("rent"),"此道具不能减免本笔费用");
            consume(room,owner,card,false,action); long reduction=card.kind.equals("immunity")?debt.amount:debt.amount/2; debt.amount-=reduction;
            log(room,"log",owner.id,owner.name+" 减免 "+paymentDescription(room,debt)+" ¥"+String.format("%,d",reduction)+"，剩余待支付 ¥"+String.format("%,d",debt.amount));
            BoardEffect.cue(room,card.kind.equals("immunity")?"immunity":"halfPayment",owner);
        } else log(room,"log",owner.id,owner.name+" 放弃本笔付款防御，仍需支付 "+paymentDescription(room,debt)+" ¥"+String.format("%,d",debt.amount));
        debt.defenseResolved=true;
        if(debt.amount==0) { room.pending=null; room.phase="manage"; }
    }
    /** 输入手牌身份，返回仅属于调用者的真实卡牌。 */
    static Card find(Player owner,String id) { return owner.hand.stream().filter(c -> Objects.equals(c.id,id)).findFirst().orElseThrow(() -> new GameError("该道具已不在你的手牌中")); }
    /** 输入已验证的卡牌及发动参数，消费并登记主动次数，记录已使用卡的目标和效果。 */
    public static void consume(Room room,Player owner,Card card,boolean active,Action action) { require(CrossoverService.handAllowed(room,owner,card,active),"此牌当前受到禁牌或冷却限制"); if(active) { CrossoverService.validateActive(room,owner,card); require(NetaService.allowed(room,owner,card),"卍解后最多再出两张牌，且不能使用卍解或投影"); } owner.hand.remove(card); if(active) { boolean counts=!room.activeCardUsed; NetaService.spent(room,owner); room.activeCardUsed=true; AnimeService.played(room,card); if(counts) NetaService.played(room,owner); } cardLog(room,owner,card,action); }
}
