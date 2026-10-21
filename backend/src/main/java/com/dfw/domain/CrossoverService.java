package com.dfw.domain;

import com.dfw.domain.rules.ContentCatalog;
import java.util.*;
import static com.dfw.domain.GameError.require;
import static com.dfw.domain.GameSupport.*;

/** 十七项素材扩展，复用攻击分段、私人选择及原效果队列。 */
public final class CrossoverService {
    public static final Set<String> CARDS=Set.of("asunaFlash","defenseSpecialization","revealingSwords","fusion","shadowBlade","doubleBoost","blueSilverBind","goldenCore","nuwaMend","namelessSword","kingsPressure");
    public static final Set<String> ATTACKS=Set.of("asunaFlash","revealingSwords","shadowBlade","blueSilverBind","kingsPressure");
    private static final Set<String> MOVEMENT=Set.of("fixed","remote","move","stay","bonus","swap","blast","swapAny","gravity","shadowstep","homeTeleport","commandSpell","voidTears","timeRewind","asunaFlash","doubleBoost");
    private static final Set<String> BUILDING=Set.of("freeUpgrade","fullUpgrade","geoResonance","growthLink","sereniteaPot","createBuild","remoteConstruction","shrineGrowth","timeRestore","nuwaMend");
    private static final Set<String> DEFENSE=Set.of("immunity","halfPayment","release","shield","imagineBreaker","vectorReflect","deathReturn","sealCard","hollowfication","defenseSpecialization");
    private static final Set<String> RESOURCE=Set.of("cash","gamble","recycle","discover","contract","projection","handDestruction","fusion","goldenCore");
    private static final Set<String> REPLACEMENTS=Set.of("fixed","remote","move","stay","homeTeleport","shadowstep","asunaFlash");
    private final ContentService content;
    private final EffectResolver effects;
    private final ContentCatalog catalog;
    private final RandomSource random;

    /** 输入原服务与随机源，所有新选择仍由同一房间事务持久化。 */
    public CrossoverService(ContentService content,EffectResolver effects,ContentCatalog catalog,RandomSource random) {
        this.content=content; this.effects=effects; this.catalog=catalog; this.random=random;
        effects.register("CROSS_BOOST",(room,effect) -> offerBoost(room,player(room,effect.playerId)));
    }

    /** 输入房间，返回旧快照可缺省的内部进度。 */
    public static CrossoverState state(Room room) { if(room.runtime==null) return new CrossoverState(); if(room.runtime.crossover==null) room.runtime.crossover=new CrossoverState(); return room.runtime.crossover; }

    /** 输入机制，返回与卡面无关、唯一的融合玩法类别。 */
    public static String category(String kind) { return MOVEMENT.contains(kind)?"移动":BUILDING.contains(kind)?"建设":DEFENSE.contains(kind)?"防御":RESOURCE.contains(kind)?"资源":"攻击"; }

    /** 输入玩家与状态定义，返回剩余回合数；不将弃牌视为使用手牌。 */
    public static int turns(Player owner,String type) { return owner.statuses.stream().filter(s -> s.type.equals(type)).mapToInt(s -> s.remainingTurns).max().orElse(0); }

    /** 输入冻结手牌，返回是否属于主动攻击；无名蓄力计入，旧版点燃大海仍为移动。 */
    public static boolean activeAttack(Card card) {
        return card.kind.equals("namelessSword") || AnimeService.attacks(card.kind) && !(card.kind.equals("igniteSea") && card.parameters.getOrDefault("version",0L)<2);
    }

    /** 输入玩家，返回是否允许主动攻击，包含不再使用手牌的蓄力及花瓣释放。 */
    public static boolean attackAllowed(Player owner) { return turns(owner,"ACTIVE_ATTACK_SEAL")==0; }

    /** 输入真实手牌及使用时机，核对全手牌、新版攻击、旧版UR主动限制与继承冷却。 */
    public static boolean handAllowed(Room room,Player owner,Card card,boolean active) {
        return turns(owner,"HAND_SEAL")==0 && (!active || turns(owner,"UR_ACTIVE_SEAL")==0 || !"UR".equals(card.rarity))
                && (!active || attackAllowed(owner) || !activeAttack(card))
                && (!active || card.lastUsedTurn!=room.turnNumber || card.usesSpent==0 || card.kind.equals("swordRain"))
;
    }

    /** 输入持牌人及卡牌，返回当前主动限制是否允许；不改变或消费状态。 */
    public static boolean activeAllowed(Room room,Player owner,Card card) {
        return NetaService.allowed(room,owner,card) && handAllowed(room,owner,card,true)
                && (turns(owner,"DEFENSE_SPECIALIZATION")==0 || !(card.kind.equals("bankai") && !NetaService.modern(card)) && !REPLACEMENTS.contains(card.kind) && !AnimeService.attacks(card.kind))
                && (turns(owner,"BLUE_BIND")==0 && state(room).stationaryTurn!=room.turnNumber || !(card.kind.equals("keqingRush") || card.kind.equals("bankai") && !NetaService.modern(card)) && !REPLACEMENTS.contains(card.kind));
    }

    /** 输入主动使用参数，在旧牌、投影和新牌共同入口检查防御特化及主动移动代价。 */
    public static void validateActive(Room room,Player owner,Card card) {
        require(NetaService.allowed(room,owner,card) && handAllowed(room,owner,card,true),"此牌当前受到禁牌、冷却或使用次数限制");
        if(turns(owner,"DEFENSE_SPECIALIZATION")>0) require(!(card.kind.equals("keqingRush") || card.kind.equals("bankai") && !NetaService.modern(card)) && !REPLACEMENTS.contains(card.kind) && !AnimeService.attacks(card.kind),"防御特化期间不能替代移动或主动攻击");
        if(turns(owner,"BLUE_BIND")>0 || state(room).stationaryTurn==room.turnNumber) require(!(card.kind.equals("keqingRush") || card.kind.equals("bankai") && !NetaService.modern(card)) && !REPLACEMENTS.contains(card.kind),"本回合不能主动移动");
    }

    /** 输入目标及期限，刷新同名状态；仅防御特化从施放当回合开始计数。 */
    private static void add(Room room,Player owner,String type,int count) {
        owner.statuses.removeIf(s -> s.type.equals(type)); PlayerStatus status=new PlayerStatus(); status.type=type; status.remainingTurns=count; status.appliedTurn=room.turnNumber; owner.statuses.add(status);
        log(room,owner.name+" 获得状态「"+label(type)+"」，持续 "+count+" 个个人回合");
    }

    /** 输入公开状态定义，返回对玩家有意义的名称。 */
    private static String label(String type) { return switch(type) { case "HAND_SEAL"->"光的护封剑"; case "UR_ACTIVE_SEAL"->"王的压制（UR主动限制）"; case "ACTIVE_ATTACK_SEAL"->"王的压制（禁止主动攻击）"; case "BLUE_BIND"->"蓝银缠绕"; default->"防御特化"; }; }

    /** 输入玩家，返回不泄露手牌的公开状态及剩余期限。 */
    public static List<String> labels(Room room,Player owner) {
        List<String> labels=new ArrayList<>();
        if(room.runtime==null) return labels;
        for(String type:List.of("HAND_SEAL","UR_ACTIVE_SEAL","ACTIVE_ATTACK_SEAL","BLUE_BIND","DEFENSE_SPECIALIZATION")) if(turns(owner,type)>0) labels.add(label(type)+"：剩余"+turns(owner,type)+"回合");
        if(state(room).chargeTurns.containsKey(owner.id)) labels.add("无名剑法：蓄力中");
        if(state(room).cultivated.contains(owner.id)) labels.add("已结丹");
        return labels;
    }

    /** 输入房间及地产，返回是否存在可以清除的附加效果。 */
    public static boolean attached(Room room,Tile t) {
        return t.barrier || t.meteorCharges>0 || PropertyEffects.shieldCharges(t)>0 || t.treasure || t.concertStacks>0 || t.corruptionTurns>0 || t.nzothOwnerId!=null || t.portalTileId!=null || AnimeService.iced(t) || !ConstructionService.labels(room,t.id).isEmpty() || !NetaService.labels(room,t).isEmpty();
    }

    /** 输入地产及是否仅削一层护盾，完整解除关联效果，神威地产保持原状态。 */
    public static void clear(Room room,Tile t,boolean oneShield) {
        if(PropertyEffects.frozen(t)) return;
        int shields=oneShield?Math.max(0,PropertyEffects.shieldCharges(t)-1):0;
        ConstructionService.clear(room,t); NetaService.clear(room,t);
        if(t.portalTileId!=null) { Tile paired=tile(room,t.portalTileId); if(paired!=null && !PropertyEffects.frozen(paired) && Objects.equals(paired.portalTileId,t.id)) paired.portalTileId=null; }
        t.barrier=false; t.meteorCharges=0; t.demolitionShieldCharges=shields; t.demolitionShield=shields>0; t.treasure=false; t.concertStacks=0; t.corruptionTurns=0; t.nzothOwnerId=null; t.portalTileId=null; t.zeroCasterId=null; t.zeroUntilTurn=0;
    }

    /** 输入新主动牌，特殊攻击交回原响应计划器，其余仅在合法确认后消费。 */
    public boolean use(Room room,Player owner,Card card,Action action) {
        if(!CARDS.contains(card.kind) || ATTACKS.contains(card.kind)) return false;
        switch(card.kind) {
            case "defenseSpecialization" -> { ContentService.consume(room,owner,card,true,action); add(room,owner,"DEFENSE_SPECIALIZATION",2); }
            case "doubleBoost" -> { ContentService.consume(room,owner,card,true,action); state(room).boostTurn=room.turnNumber; state(room).boosts=2; state(room).boostBlocked=false; }
            case "goldenCore" -> {
                require(!state(room).cultivated.contains(owner.id),"本局已经成功结丹"); ContentService.consume(room,owner,card,true,action); state(room).corePlayerId=owner.id; stationary(room);
            }
            case "namelessSword" -> {
                require(!state(room).chargeTurns.containsKey(owner.id),"已有无名剑法蓄力"); ContentService.consume(room,owner,card,true,action);
                state(room).chargedCards.put(owner.id,AnimeService.copy(card)); state(room).chargeTurns.put(owner.id,owner.personalTurns+1); stationary(room);
            }
            case "fusion" -> {
                require(owner.hand.size()>=3,"融合需要额外两张材料牌");
                ContentChoice c=content.open(room,owner,card.fusionResult==null?"cross_fusion_materials":"cross_fusion_confirm"); c.cardId=card.id; c.content=card;
                if(card.fusionResult==null) { c.cards.addAll(owner.hand.stream().filter(x -> !x.id.equals(card.id)).toList()); c.prompt="依次选择第一、第二张材料；第一张决定优先融合类别。"; }
                else { c.cards.add(card.fusionResult); c.materialIds.addAll(card.fusionMaterials); c.prompt="融合结果已经固定，确认后消耗融合及两张材料。"; }
            }
            case "nuwaMend" -> {
                List<Integer> targets=repairTargets(room,owner); require(!targets.isEmpty(),"最近一次攻击或灾害没有可修复的自家地产");
                ContentChoice c=content.open(room,owner,"cross_mend"); c.cardId=card.id; c.content=card; c.tileIds.addAll(targets); c.prompt="从最近一次实际受损的地产中选择最多三处修复。";
            }
            default -> throw new GameError("素材主动牌尚未注册");
        }
        return true;
    }

    /** 输入当前主动窗口，放弃本回合移动但保留后续资产管理。 */
    private static void stationary(Room room) { state(room).stationaryTurn=room.turnNumber; room.runtime.rollMode="STATIONARY"; room.runtime.windowExit="replace_roll"; }

    /** 输入原攻击计划，固定路径、双目标或单个禁牌目标，不立即执行。 */
    public boolean plan(Room room,Player actor,Card card,Action action,List<GameEffect> parts) {
        switch(card.kind) {
            case "asunaFlash" -> {
                require(action.point!=null && action.point>=1 && action.point<=3,"请选择突进1～3格");
                for(int i=1;i<=action.point;i++) { Tile t=room.board.get((actor.position+i)%room.board.size()); if(!PropertyEffects.frozen(t)) part(parts,room,actor,player(room,t.ownerId),t,"cross_clear",1); }
                part(parts,room,actor,actor,null,"cross_flash",action.point);
            }
            case "shadowBlade" -> {
                require(action.tileIds!=null && action.tileIds.size()==2 && new HashSet<>(action.tileIds).size()==2,"请选择相邻的两处不同对手地产");
                Tile first=tile(room,action.tileIds.getFirst()), second=tile(room,action.tileIds.getLast());
                require(enemy(room,actor,first) && enemy(room,actor,second) && nearbyDistance(first.id,second.id,room.board.size())==1,"双龙目标必须紧邻且属于对手，并有建筑");
                part(parts,room,actor,player(room,first.ownerId),first,"lower",1); part(parts,room,actor,player(room,second.ownerId),second,"lower",1);
            }
            case "revealingSwords","kingsPressure","blueSilverBind" -> {
                Player target=player(room,action.playerId); require(target!=null && !target.bankrupt && target!=actor,"请选择未破产对手");
                if(card.kind.equals("blueSilverBind")) require(nearbyDistance(actor.position,target.position,room.board.size())<=6,"缠绕目标须在前后六格内");
                part(parts,room,actor,target,null,card.kind.equals("kingsPressure") && card.parameters.getOrDefault("version",0L)>=2?"cross_kingsPressureAttack":"cross_"+card.kind,1);
            }
            case "namelessSword" -> { Tile target=tile(room,action.tileId); require(enemy(room,actor,target) && nearbyDistance(actor.position,target.id,room.board.size())<=6,"请选择前后六格内有建筑的对手地产"); part(parts,room,actor,player(room,target.ownerId),target,"lower",3); }
            default -> { return false; }
        }
        return true;
    }

    /** 输入一段固定目标，沿用原攻击队列和防御响应。 */
    private void part(List<GameEffect> parts,Room room,Player actor,Player target,Tile tile,String selector,long amount) {
        GameEffect e=effects.effect("ANIME_PART",actor); e.targetId=target==null?null:target.id; e.tileId=tile==null?null:tile.id; e.propertyTarget=tile!=null; e.selector=selector; e.amount=amount; parts.add(e);
    }

    /** 输入地产，返回可被建筑攻击的对手目标。 */
    private static boolean enemy(Room room,Player owner,Tile t) { return t!=null && t.type.equals("property") && !PropertyEffects.frozen(t) && t.level>0 && t.ownerId!=null && !t.ownerId.equals(owner.id) && !player(room,t.ownerId).bankrupt; }

    /** 输入已经响应的攻击分段，施加清除、突进模式或定时限制。 */
    public void resolvePart(Room room,GameEffect e) {
        Player target=player(room,e.targetId);
        switch(e.selector) {
            case "cross_clear" -> clear(room,tile(room,e.tileId),true);
            case "cross_flash" -> { room.runtime.rollMode="FLASH"; room.runtime.movementSteps=(int)e.amount; room.dice=(int)e.amount; room.runtime.windowExit="replace_roll"; }
            case "cross_revealingSwords" -> add(room,target,"HAND_SEAL",3);
            case "cross_kingsPressure" -> add(room,target,"UR_ACTIVE_SEAL",3);
            case "cross_kingsPressureAttack" -> add(room,target,"ACTIVE_ATTACK_SEAL",3);
            case "cross_blueSilverBind" -> {
                if(StatusService.controlProtectionTurns(target)>0) { log(room,target.name+" 的连续控制保护抵挡蓝银缠绕"); return; }
                List<Card> release=target.hand.stream().filter(c -> c.kind.equals("release") && handAllowed(room,target,c,false)).toList();
                if(release.isEmpty()) bind(room,target);
                else { ContentChoice c=content.open(room,target,"cross_bind_release"); c.cards.addAll(release); c.prompt="可以使用解控牌解除蓝银缠绕；接受后仅限制下次个人回合移动。"; }
            }
            default -> throw new GameError("未知素材攻击分段");
        }
    }

    /** 输入实际被缠绕玩家，登记移动限制及原有连续控制保护。 */
    private static void bind(Room room,Player target) { add(room,target,"BLUE_BIND",1); StatusService.protectControl(room,target); }

    /** 输入抽到的事件，固定随机目标后执行或开放原私人选择。 */
    public boolean event(Room room,Player owner,ContentResult source) {
        switch(source.mechanism) {
            case "aquaPurify" -> {
                List<Tile> targets=new ArrayList<>(room.board.stream().filter(t -> !PropertyEffects.frozen(t) && attached(room,t)).toList());
                for(int i=0;i<3 && !targets.isEmpty();i++) { Tile t=targets.remove(random.nextInt(targets.size())); clear(room,t,false); log(room,"阿库娅净化了 "+t.name+" 的全部附加效果"); }
            }
            case "worldSeed" -> {
                List<Tile> targets=new ArrayList<>(room.board.stream().filter(t -> ConstructionService.eligible(room,owner,t)).toList());
                for(int i=0;i<2 && !targets.isEmpty();i++) ConstructionService.build(room,owner,targets.remove(random.nextInt(targets.size())));
            }
            case "nzothWrath" -> {
                List<Player> owners=new ArrayList<>(room.players.stream().filter(p -> !p.bankrupt && room.board.stream().anyMatch(t -> Objects.equals(t.ownerId,p.id) && !PropertyEffects.frozen(t) && t.level>0) && room.board.stream().noneMatch(t -> Objects.equals(t.nzothOwnerId,p.id))).toList());
                for(int i=0;i<3 && !owners.isEmpty();i++) { Player p=owners.remove(random.nextInt(owners.size())); List<Tile> targets=room.board.stream().filter(t -> Objects.equals(t.ownerId,p.id) && t.level>0 && !PropertyEffects.frozen(t)).toList(); Tile t=targets.get(random.nextInt(targets.size())); t.nzothOwnerId=p.id; log(room,t.name+" 受到恩佐斯污染，业主可在本人回合弃牌净化"); }
            }
            case "endlessClouds" -> { for(Player p:room.players) if(!p.bankrupt) p.cash=Math.addExact(p.cash,400); cleanse(room,owner); log(room,"白云无尽：全员获得 ¥400，"+owner.name+" 及其地产完成净化"); }
            case "pinkMiracle" -> {
                if(owner.hand.isEmpty()) owner.cash=Math.addExact(owner.cash,800);
                else { ContentChoice c=content.open(room,owner,"cross_pink_materials"); c.content=source; c.cards.addAll(owner.hand); c.prompt="选择最多三张任意手牌，按原稀有度重新搭配，不升级。"; }
            }
            case "darkGame" -> {
                List<String> targets=room.players.stream().filter(p -> p!=owner && !p.bankrupt && p.hand.stream().anyMatch(c -> c.expiresAtTurn==0)).map(p -> p.id).toList();
                if(owner.hand.stream().noneMatch(c -> c.expiresAtTurn==0) || targets.isEmpty()) return true;
                CrossoverState.Duel duel=new CrossoverState.Duel(); duel.actorId=owner.id; state(room).duel=duel;
                ContentChoice c=content.open(room,owner,"cross_duel_target"); c.content=source; c.playerIds.addAll(targets); c.prompt="邀请一名对手进行自愿暗押手牌赌局，也可以放弃。";
            }
            default -> { return false; }
        }
        return true;
    }

    /** 输入踩格者，仅清除其临时负面状态，保留正面效果、永久变化和自愿代价。 */
    private static void cleanse(Room room,Player owner) {
        boolean released=owner.skipTurns>owner.voluntarySkipTurns || turns(owner,"BLUE_BIND")>0;
        owner.skipTurns=owner.voluntarySkipTurns; if(owner.voluntarySkipTurns==0) owner.jailed=false;
        if(released) StatusService.controlledSkip(room,owner);
        owner.statuses.removeIf(s -> Set.of("NEXT_ROLL_HALF","NEXT_RENT_DOUBLE","BLUE_BIND","HAND_SEAL","UR_ACTIVE_SEAL","ACTIVE_ATTACK_SEAL").contains(s.type));
        for(Tile t:room.board) if(Objects.equals(t.ownerId,owner.id)) {
            t.corruptionTurns=0; t.nzothOwnerId=null;
            if(t.petalCasterId!=null && !t.petalCasterId.equals(owner.id)) { t.petalCasterId=null; t.petalOwnerId=null; t.petalUntilTurn=0; }
            if(room.runtime.neta!=null) room.runtime.neta.thunder.entrySet().removeIf(entry -> entry.getValue().tileId==t.id && !entry.getKey().equals(owner.id));
            if(t.zeroCasterId!=null && !t.zeroCasterId.equals(owner.id)) { t.zeroCasterId=null; t.zeroUntilTurn=0; }
            if(t.kamuiCasterId!=null && !t.kamuiCasterId.equals(owner.id)) { t.kamuiCasterId=null; t.kamuiTurns=0; t.kamuiAppliedTurn=0; if(t.portalTileId!=null) { Tile paired=tile(room,t.portalTileId); if(paired==null || !Objects.equals(paired.portalTileId,t.id)) t.portalTileId=null; } }
        }
    }

    /** 输入原子效果，固定同一次主动攻击或灾害的损坏组，不改变旧修复记录。 */
    public static void damageSource(Room room,GameEffect effect) {
        CrossoverState s=state(room); s.damageBatch=room.turnNumber+":"+(effect.content==null?effect.id:effect.content.id);
        s.damageActorId=effect.content!=null && "card".equals(effect.content.pool)?effect.playerId:null;
    }

    /** 输入受损地产，记录同组首次受损前等级，仅允许修复敌方或事件实际损坏。 */
    public static void recordDamage(Room room,Tile t) {
        CrossoverState s=state(room); Player owner=player(room,t.ownerId); if(owner==null || owner.bankrupt || t.level<=0) return;
        if(owner.id.equals(s.damageActorId)) { s.damage.remove(t.id); return; }
        String batch=s.damageBatch==null?"turn:"+room.turnNumber:s.damageBatch;
        CrossoverState.Damage d=s.damage.get(t.id);
        if(d==null || !d.batch.equals(batch) || !d.ownerId.equals(owner.id)) { d=new CrossoverState.Damage(); d.ownerId=owner.id; d.batch=batch; d.level=t.level; d.untilTurn=owner.personalTurns+1; s.damage.put(t.id,d); }
        s.lastDamageBatch.put(owner.id,batch);
    }

    /** 输入本人，返回最近同一次攻击中产权未变且仍可修复的地产。 */
    private static List<Integer> repairTargets(Room room,Player owner) {
        CrossoverState s=state(room); return s.damage.entrySet().stream().filter(e -> {
            Tile t=tile(room,e.getKey()); CrossoverState.Damage d=e.getValue();
            return Objects.equals(d.ownerId,owner.id) && Objects.equals(d.batch,s.lastDamageBatch.get(owner.id)) && owner.personalTurns<=d.untilTurn && AnimeService.restorable(room,owner,t) && t.level<d.level;
        }).map(Map.Entry::getKey).toList();
    }

    /** 输入本人管理或掷骰前窗口，开放弃牌净化，不消费主动次数或使用材料效果。 */
    public void openPurify(Room room,Player owner) {
        require(current(room)==owner && room.pending==null && room.trade==null && Set.of("pre_roll","manage").contains(room.phase),"只能在本人回合空闲窗口净化");
        List<Integer> targets=room.board.stream().filter(t -> Objects.equals(t.ownerId,owner.id) && Objects.equals(t.nzothOwnerId,owner.id)).map(t -> t.id).toList();
        require(!targets.isEmpty() && !owner.hand.isEmpty(),"没有可净化污染或可弃置手牌");
        ContentChoice c=content.open(room,owner,"cross_purify"); c.tileIds.addAll(targets); c.cards.addAll(owner.hand); c.prompt="弃一张任意手牌净化一处自家污染，不折现，不占主动次数。";
    }

    /** 输入当前普通移动完成的玩家，在落点前逐次开放喷射，不重新掷骰。 */
    public void offerBoost(Room room,Player owner) {
        CrossoverState s=state(room); if(s.boostTurn!=room.turnNumber || s.boosts<=0 || s.boostBlocked) return;
        ContentChoice c=content.open(room,owner,"cross_boost"); c.prompt="当前位置："+room.board.get(owner.position).name+"。可追加前进1格，剩余 "+s.boosts+" 次；陨星不拦截喷射。";
    }

    /** 输入当前主动窗口，保存一次蓄力释放选择；跳过回合不延期。 */
    public boolean offerCharge(Room room) {
        CrossoverState s=state(room); Player owner=current(room);
        if(!Objects.equals(s.chargeTurns.get(owner.id),owner.personalTurns) || s.chargeOfferedTurn==room.turnNumber) return false;
        s.chargeOfferedTurn=room.turnNumber;
        List<Integer> targets=room.board.stream().filter(t -> enemy(room,owner,t) && nearbyDistance(owner.position,t.id,room.board.size())<=6).map(t -> t.id).toList();
        if(targets.isEmpty() || !attackAllowed(owner) || turns(owner,"DEFENSE_SPECIALIZATION")>0) { s.chargeTurns.remove(owner.id); s.chargedCards.remove(owner.id); return false; }
        ContentChoice c=content.open(room,owner,"cross_charge"); c.tileIds.addAll(targets); c.prompt="选择前后六格内一处对手地产释放三道剑气，不占本回合主动次数；放弃则蓄力消失。"; return true;
    }

    /** 输入即将结束的回合，先净化选择，再一次性腐蚀和结丹，等待超限完成后才能换人。 */
    public boolean beforeEnd(Room room,Player owner) {
        CrossoverState s=state(room);
        if(s.endPreparedTurn!=room.turnNumber) {
            s.endPreparedTurn=room.turnNumber;
            List<Integer> polluted=room.board.stream().filter(t -> Objects.equals(t.nzothOwnerId,owner.id) && Objects.equals(t.ownerId,owner.id)).map(t -> t.id).toList();
            if(!polluted.isEmpty() && !owner.hand.isEmpty()) { ContentChoice c=content.open(room,owner,"cross_purify"); c.tileIds.addAll(polluted); c.cards.addAll(owner.hand); c.prompt="回合即将结束，可弃一张手牌净化一处污染，否则本回合腐蚀一次。"; return true; }
        }
        if(s.endAppliedTurn!=room.turnNumber) {
            s.endAppliedTurn=room.turnNumber; s.damageBatch="nzoth:"+room.turnNumber; s.damageActorId=null;
            for(Tile t:room.board) if(Objects.equals(t.nzothOwnerId,owner.id) && Objects.equals(t.ownerId,owner.id) && !PropertyEffects.frozen(t)) {
                int shield=PropertyEffects.shieldCharges(t);
                if(shield>0) { t.demolitionShieldCharges=shield-1; t.demolitionShield=shield>1; log(room,t.name+" 的污染腐蚀一层护盾"); }
                else if(t.level>0) { AnimeService.recordDamage(room,t); t.level--; log(room,t.name+" 的污染使建筑降低一级"); }
                if(t.level==0 && PropertyEffects.shieldCharges(t)==0) t.nzothOwnerId=null;
            }
            if(Objects.equals(s.corePlayerId,owner.id) && !owner.bankrupt) {
                s.corePlayerId=null; s.cultivated.add(owner.id); owner.handLimitReduction=5-PropertyEffects.handLimit(owner);
                Card reward=ContentService.card(catalog.drawTier(room,"SSR",null,Set.of(),random)); owner.hand.add(reward); owner.contentNotice=reward; owner.contentNoticeText="结丹成功，手牌上限增加一，获得SSR道具"; log(room,owner.name+" 结丹成功，手牌上限增加一");
            }
            if(content.overflow(room)) return true;
        }
        return false;
    }

    /** 输入结束者，递减素材状态并清理到期蓄力；已完成的普通回合和跳过回合共用。 */
    public static void endTurn(Room room,Player owner) {
        for(PlayerStatus s:owner.statuses) if(Set.of("HAND_SEAL","UR_ACTIVE_SEAL","ACTIVE_ATTACK_SEAL","BLUE_BIND","DEFENSE_SPECIALIZATION").contains(s.type) && (s.type.equals("DEFENSE_SPECIALIZATION") || room.turnNumber>s.appliedTurn)) s.remainingTurns--;
        owner.statuses.removeIf(s -> Set.of("HAND_SEAL","UR_ACTIVE_SEAL","ACTIVE_ATTACK_SEAL","BLUE_BIND","DEFENSE_SPECIALIZATION").contains(s.type) && s.remainingTurns<=0);
        if(state(room).chargeTurns.getOrDefault(owner.id,Integer.MAX_VALUE)<=owner.personalTurns) { state(room).chargeTurns.remove(owner.id); state(room).chargedCards.remove(owner.id); }
    }

    /** 输入恢复或玩家移出后的房间，取消失去等待位置或参与者的赌局。 */
    public static void resume(Room room) {
        CrossoverState.Duel duel=state(room).duel; if(duel==null) return;
        Player actor=player(room,duel.actorId), target=player(room,duel.targetId);
        if(actor==null || actor.bankrupt || duel.targetId!=null && (target==null || target.bankrupt)) {
            if(room.runtime.choice!=null && room.runtime.choice.type.startsWith("cross_duel")) close(room,room.runtime.choice);
            state(room).duel=null; log(room,"黑暗游戏参与者已退出，未转移任何押牌");
        } else if(room.runtime.choice==null) state(room).duel=null;
    }

    /** 输入选择，恢复父等待，后续选择继续复用原效果队列。 */
    private static void close(Room room,ContentChoice c) { room.pending=c.returnPending; room.phase=c.returnPhase; room.runtime.choice=null; }

    /** 输入材料身份，复核同一选择的真实手牌、数量和顺序。 */
    private static List<Card> materials(Player owner,ContentChoice c,List<String> ids,int min,int max) {
        require(ids!=null && ids.size()>=min && ids.size()<=max && new HashSet<>(ids).size()==ids.size() && c.cards.stream().filter(x -> ids.contains(x.id)).count()==ids.size(),"请选择合法的不同材料牌");
        return ids.stream().map(id -> ContentService.find(owner,id)).toList();
    }

    /** 输入素材选择，固定候选并原子替换；失败保留原牌、选择和主动次数。 */
    public boolean decide(Room room,Player owner,ContentChoice c,Action action) {
        if(!c.type.startsWith("cross_")) return false;
        require(action.accept!=null,"请选择确认或放弃");
        if(!action.accept) {
            if(c.type.equals("cross_boost")) state(room).boosts=0;
            if(c.type.equals("cross_charge")) { state(room).chargeTurns.remove(owner.id); state(room).chargedCards.remove(owner.id); }
            if(c.type.equals("cross_bind_release")) bind(room,owner);
            if(c.type.startsWith("cross_duel")) state(room).duel=null;
            close(room,c); return true;
        }
        switch(c.type) {
            case "cross_fusion_materials" -> {
                List<Card> chosen=materials(owner,c,action.discardCardIds,2,2); Card source=ContentService.find(owner,c.cardId); validateActive(room,owner,source);
                if(source.fusionResult==null) { source.fusionMaterials=new ArrayList<>(action.discardCardIds); source.fusionResult=ContentService.card(catalog.drawTier(room,"UR",category(chosen.getFirst().kind),Set.of(),random)); }
                require(source.fusionMaterials.equals(action.discardCardIds),"融合结果已经固定，请使用原有材料");
                c.type="cross_fusion_confirm"; c.materialIds=new ArrayList<>(source.fusionMaterials); c.cards=new ArrayList<>(List.of(source.fusionResult)); c.prompt="结果已固定，确认后消耗融合和两张材料。"; return true;
            }
            case "cross_fusion_confirm" -> {
                Card source=ContentService.find(owner,c.cardId); validateActive(room,owner,source);
                require(source.fusionResult!=null && source.fusionMaterials.equals(c.materialIds) && c.materialIds.size()==2 && !c.materialIds.contains(source.id),"融合材料或结果已失效");
                List<Card> chosen=c.materialIds.stream().map(id -> ContentService.find(owner,id)).toList(); ContentService.consume(room,owner,source,true,action); owner.hand.removeAll(chosen); owner.hand.add(source.fusionResult); owner.contentNotice=source.fusionResult; owner.contentNoticeText="融合获得UR道具";
            }
            case "cross_pink_materials" -> {
                List<Card> chosen=materials(owner,c,action.discardCardIds,1,3); c.materialIds=new ArrayList<>(action.discardCardIds);
                for(Card source:chosen) { List<Card> options=new ArrayList<>(); Set<String> excluded=new HashSet<>(); for(int i=0;i<3;i++) { Card result=ContentService.card(catalog.drawTier(room,source.rarity==null?AnimeService.copy(source).rarity:source.rarity,null,excluded,random)); options.add(result); excluded.add(result.kind+":"+result.parameterId); } c.cardOptions.put(source.id,options); }
                c.type="cross_pink_pick"; c.cards=new ArrayList<>(chosen); c.prompt="每张材料选择一个同档效果，候选已经固定，不升级。"; return true;
            }
            case "cross_pink_pick" -> {
                require(action.cardSelections!=null && action.cardSelections.size()==c.materialIds.size() && action.cardSelections.keySet().containsAll(c.materialIds),"请为每张材料选择一个候选");
                List<Card> old=new ArrayList<>(), replacements=new ArrayList<>();
                for(String id:c.materialIds) { Card source=ContentService.find(owner,id); Card replacement=c.cardOptions.get(id).stream().filter(x -> x.id.equals(action.cardSelections.get(id))).findFirst().orElseThrow(() -> new GameError("重塑候选已失效"));
                    replacement.expiresAtTurn=source.expiresAtTurn; replacement.projected=source.projected; replacement.lastUsedTurn=source.lastUsedTurn; replacement.usesSpent=Math.max(source.usesSpent,source.kind.equals("swordRain") && source.remainingUses==1?1:0); if(replacement.kind.equals("swordRain")) replacement.remainingUses=Math.max(0,2-replacement.usesSpent); old.add(source); replacements.add(replacement); }
                owner.hand.removeAll(old); owner.hand.addAll(replacements); owner.contentNotice=replacements.getLast(); owner.contentNoticeText="奇迹搭配完成，手牌等级和数量保持不变";
            }
            case "cross_mend" -> {
                List<Integer> ids=action.tileIds; require(ids!=null && !ids.isEmpty() && ids.size()<=3 && new HashSet<>(ids).size()==ids.size() && c.tileIds.containsAll(ids) && repairTargets(room,owner).containsAll(ids),"请选择最近一次损坏中最多三处合法地产");
                Card source=ContentService.find(owner,c.cardId); validateActive(room,owner,source); ContentService.consume(room,owner,source,true,action);
                for(int id:ids) { Tile t=tile(room,id); t.level=state(room).damage.get(id).level; t.damageOwnerId=null; state(room).damage.remove(id); log(room,t.name+" 经女娲补天恢复到 "+t.level+" 级"); }
            }
            case "cross_purify" -> {
                require(current(room)==owner && c.tileIds.contains(action.tileId),"只能在本人回合净化原候选污染"); Tile t=tile(room,action.tileId); require(Objects.equals(t.nzothOwnerId,owner.id) && Objects.equals(t.ownerId,owner.id),"污染产权已变化");
                Card material=ContentService.find(owner,action.cardId); require(c.cards.stream().anyMatch(x -> x.id.equals(material.id)),"请选择原候选材料"); owner.hand.remove(material); t.nzothOwnerId=null; log(room,owner.name+" 弃置一张手牌净化 "+t.name);
            }
            case "cross_bind_release" -> { Card card=ContentService.find(owner,action.cardId); require(card.kind.equals("release"),"请选择解控牌"); ContentService.consume(room,owner,card,false,action); }
            case "cross_boost" -> {
                require(state(room).boostTurn==room.turnNumber && state(room).boosts>0 && !state(room).boostBlocked,"喷射机会已经结束"); state(room).boosts--; close(room,c);
                GameEffect move=effects.effect("MOVE",owner); move.amount=1; move.selector="BOOST"; move.resumesDecision=true; GameEffect next=effects.effect("CROSS_BOOST",owner); next.resumesDecision=true; effects.submitAll(room,List.of(move,next)); return true;
            }
            case "cross_charge" -> {
                require(c.tileIds.contains(action.tileId) && Objects.equals(state(room).chargeTurns.get(owner.id),owner.personalTurns),"蓄力目标已失效");
                Card charged=state(room).chargedCards.get(owner.id); close(room,c); content.anime.attack(room,owner,charged,action,false); state(room).chargeTurns.remove(owner.id); state(room).chargedCards.remove(owner.id); return true;
            }
            case "cross_duel_target" -> {
                require(c.playerIds.contains(action.playerId) && player(room,action.playerId)!=null && !player(room,action.playerId).bankrupt,"请选择合法对手"); state(room).duel.targetId=action.playerId; c.type="cross_duel_stake"; c.cards=new ArrayList<>(owner.hand.stream().filter(x -> x.expiresAtTurn==0).toList()); c.prompt="选择一张非临时手牌暗押；对方同意前不公开。"; return true;
            }
            case "cross_duel_stake" -> {
                Card stake=ContentService.find(owner,action.cardId); require(stake.expiresAtTurn==0 && c.cards.stream().anyMatch(x -> x.id.equals(stake.id)),"请选择非临时手牌"); CrossoverState.Duel duel=state(room).duel;
                if(owner.id.equals(duel.actorId)) {
                    duel.actorCardId=stake.id; Player target=player(room,duel.targetId); close(room,c);
                    if(target==null || target.bankrupt || target.hand.stream().noneMatch(x -> x.expiresAtTurn==0)) { state(room).duel=null; return true; }
                    ContentChoice next=content.open(room,target,"cross_duel_stake"); next.cards.addAll(target.hand.stream().filter(x -> x.expiresAtTurn==0).toList()); next.prompt=owner.name+" 邀请暗押一张手牌进行黑暗游戏；可以拒绝，双方各掷一次骰，平局退回。"; return true;
                }
                Player actor=player(room,duel.actorId); require(actor!=null && !actor.bankrupt,"发起人已退出赌局"); Card first=ContentService.find(actor,duel.actorCardId);
                duel.actorDie=random.nextInt(6)+1; duel.targetDie=random.nextInt(6)+1;
                if(!duel.actorDie.equals(duel.targetDie)) { actor.hand.remove(first); owner.hand.remove(stake); Player winner=duel.actorDie>duel.targetDie?actor:owner; winner.hand.add(first); winner.hand.add(stake); winner.contentNotice=winner==actor?stake:first; winner.contentNoticeText="黑暗游戏获得手牌"; }
                log(room,"黑暗游戏揭晓："+actor.name+" "+duel.actorDie+" 点，"+owner.name+" "+duel.targetDie+" 点"+(duel.actorDie.equals(duel.targetDie)?"，平局退回":"，胜者获得双方押牌")); state(room).duel=null;
            }
            default -> throw new GameError("素材选择尚未注册");
        }
        close(room,c); content.overflow(room); return true;
    }
}
