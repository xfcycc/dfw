package com.dfw.domain;

import java.util.*;
import static com.dfw.domain.GameError.require;
import static com.dfw.domain.GameSupport.*;

/** 动漫道具的固定效果计划、防御响应及回合状态；全部复用房间事务与私人选择。 */
public final class AnimeService {
    /** 有独立使用时机的手牌，禁止作为普通主动牌消耗。 */
    public static final Set<String> DEFENSE = Set.of("timeRewind","imagineBreaker","vectorReflect","sealCard","deathReturn","hollowfication");
    /** 需要按目标分段并允许主动防御的牌型。 */
    private static final Set<String> ATTACKS = Set.of("barrier","demolish","fullDemolish","swap","blast","jail","steal","exchange","kamui","blackhole","sap","slow","reaper","swapAny","handDestruction","gravity","meteor","starburst","corruption","commandSpell","sevenSwords","railgun","explosion","swordRain","voidTears","absoluteZero");
    private final ContentService content;
    private final EffectResolver effects;
    private final RandomSource random;
    /** 输入原内容服务、效果队列和随机源，注册固定分段与响应结束处理器。 */
    public AnimeService(ContentService content, EffectResolver effects, RandomSource random) {
        this.content=content; this.effects=effects; this.random=random;
        effects.register("ANIME_PART",this::resolve);
        effects.register("ANIME_FINISH",(room,effect) -> { AnimeState.Attack attack=state(room).attack; if(attack!=null && room.pending==null) room.phase=attack.returnPhase; state(room).attack=null; content.overflow(room); });
    }
    /** 输入房间，返回旧同格式存档也可使用的内部扩展状态。 */
    public static AnimeState state(Room room) { if(room.runtime.anime==null) room.runtime.anime=new AnimeState(); return room.runtime.anime; }
    /** 输入已消费主动牌，保存完整公开记录供下一回合投影，返回空。 */
    public static void played(Room room,Card card) { Card record=copy(card); if(record.cardType==null) record.cardType="主动"; state(room).currentCards.add(record); }
    /** 输入完整手牌，复制效果及有期限的实例状态，不共享可变参数。 */
    public static Card copy(Card source) {
        Card card=ContentService.card(source); card.kind=source.kind; if(card.mechanism==null) card.mechanism=source.kind;
        if(card.pool==null) card.pool="card"; if(card.rarity==null) card.rarity=Set.of("barrier","remote","stay","immunity").contains(source.kind)?"SSR":"SR";
        if(card.name==null) card.name=switch(source.kind) { case "barrier"->"路障"; case "demolish"->"拆房"; case "swap"->"换位"; default->source.kind; };
        card.expiresAtTurn=source.expiresAtTurn; card.projected=source.projected;
        card.usesSpent=source.usesSpent; card.remainingUses=source.remainingUses; card.lastUsedTurn=source.lastUsedTurn; return card;
    }
    /** 输入卡型，返回是否由目标计划器负责。 */
    public static boolean attacks(String kind) { return ATTACKS.contains(kind) || CrossoverService.ATTACKS.contains(kind) || Set.of("keqingRush","thunderLord","fireBurial","igniteSea","senbonzakura").contains(kind); }
    /** 输入持牌人和当前牌，返回是否允许使用其专属投影次数或剑雨剩余次数。 */
    public static boolean usable(Room room,Card card) {
        return NetaService.allowed(room,current(room),card) && (!room.activeCardUsed || NetaService.bankaiActive(room,current(room)) && NetaService.state(room).bankaiRemaining>0 || card.projected && card.expiresAtTurn==room.turnNumber) && (!card.kind.equals("swordRain") || card.lastUsedTurn!=room.turnNumber && card.usesSpent<2);
    }
    /** 输入新回合房间，保存回归点并依次处理业主的腐败，跳过回合也计数。 */
    public void startTurn(Room room) {
        AnimeState state=state(room); if(state.startedTurn==room.turnNumber) return;
        state.startedTurn=room.turnNumber; Player owner=current(room); owner.personalTurns++; owner.turnStartPosition=owner.position;
        observeOwnership(room);
        content.neta.startTurn(room,owner);
        for(Tile tile:room.board) if(Objects.equals(tile.ownerId,owner.id) && tile.corruptionTurns>0 && !PropertyEffects.frozen(tile)) {
            CrossoverService.state(room).damageBatch="corruption:"+tile.id+":"+room.turnNumber; CrossoverService.state(room).damageActorId=null;
            GameEffect damage=effects.effect("NETA_DAMAGE",owner); damage.tileId=tile.id; damage.amount=1; damage.selector="corruption"; effects.submit(room,damage);
        }
    }
    /** 输入结束行动者，销毁本回合临时牌，更新投影记录并解除到期零域。 */
    public static void endTurn(Room room,Player owner) {
        NetaService.endTurn(room,owner);
        AnimeState state=state(room);
        for(Player p:room.players) p.hand.removeIf(c -> c.expiresAtTurn>0 && c.expiresAtTurn<=room.turnNumber);
        state.previousCards=new ArrayList<>(state.currentCards); state.currentCards.clear();
        for(Tile tile:room.board) if(Objects.equals(tile.zeroCasterId,owner.id) && owner.personalTurns>=tile.zeroUntilTurn) { tile.zeroCasterId=null; tile.zeroUntilTurn=0; }
    }
    /** 输入房间，产权改变后立即清除旧修复证据，不能转手再转回恢复旧记录。 */
    public static void observeOwnership(Room room) {
        for(Tile tile:room.board) if(tile.nzothOwnerId!=null && tile.level==0 && PropertyEffects.shieldCharges(tile)==0) tile.nzothOwnerId=null;
        NetaService.clean(room);
        for(Tile tile:room.board) if(!Objects.equals(tile.observedOwnerId,tile.ownerId)) {
            tile.damageOwnerId=null; tile.damageLevel=0; tile.damageUntilTurn=0; tile.observedOwnerId=tile.ownerId;
            CrossoverService.state(room).damage.remove(tile.id);
            if(!Objects.equals(tile.nzothOwnerId,tile.ownerId)) tile.nzothOwnerId=null;
        }
    }
    /** 输入受损地产，记录实际损坏前等级和业主的下一个个人回合期限。 */
    public static void recordDamage(Room room,Tile tile) {
        Player owner=player(room,tile.ownerId); if(owner==null || owner.bankrupt || tile.level<=0) return;
        CrossoverService.recordDamage(room,tile);
        tile.damageOwnerId=owner.id; tile.damageLevel=tile.level; tile.damageUntilTurn=owner.personalTurns+1; tile.observedOwnerId=owner.id;
    }
    /** 输入房间与地产，返回是否可用四之弹恢复，排除未实际损坏及产权变化。 */
    public static boolean restorable(Room room,Player owner,Tile tile) {
        return tile!=null && tile.type.equals("property") && !PropertyEffects.frozen(tile) && Objects.equals(tile.ownerId,owner.id) && Objects.equals(tile.damageOwnerId,owner.id) && tile.damageLevel>tile.level && owner.personalTurns<=tile.damageUntilTurn;
    }
    /** 输入地产，返回零域是否仍生效；此状态只停止收租及升级。 */
    public static boolean iced(Tile tile) { return tile!=null && tile.zeroCasterId!=null && tile.zeroUntilTurn>0; }
    /** 输入房间及普通骰点，在移动前向持有人开放一次重掷选择，返回是否等待。 */
    public boolean offerRewind(Room room) {
        AnimeState state=state(room); Player owner=current(room);
        if(state.rewindOfferedTurn==room.turnNumber || owner.hand.stream().noneMatch(c -> c.kind.equals("timeRewind") && CrossoverService.handAllowed(room,owner,c,false))) return false;
        state.rewindOfferedTurn=room.turnNumber;
        ContentChoice choice=content.open(room,owner,"anime_rewind"); choice.cards.addAll(owner.hand.stream().filter(c -> c.kind.equals("timeRewind") && CrossoverService.handAllowed(room,owner,c,false)).toList());
        choice.prompt="本次普通骰点为 "+room.dice+"，可以使用时间溯行重新掷一次，必须接受新结果。"; return true;
    }
    /** 输入主动牌和参数，处理投影、契约、修复或完整攻击计划；无合法目标不消费。 */
    public boolean use(Room room,Player actor,Card card,Action action) {
        if(card.kind.equals("projection")) {
            List<Card> candidates=state(room).previousCards.stream().filter(c -> !c.kind.equals("projection") && "主动".equals(c.cardType)).toList();
            require(!candidates.isEmpty(),"上一回合没有可投影的主动道具");
            ContentChoice choice=content.open(room,actor,"anime_projection"); choice.cardId=card.id; choice.content=card; choice.cards.addAll(candidates); choice.prompt="选择上一回合使用过的道具；投影牌本回合可额外使用一次，回合结束消失。"; return true;
        }
        if(card.kind.equals("contract")) {
            require(PropertyEffects.handLimit(actor)>1,"手牌上限已为一张，不能签订契约"); ContentService.consume(room,actor,card,true,action);
            actor.cash=Math.addExact(actor.cash,5000); actor.handLimitReduction++; log(room,actor.name+" 契约成立，获得 ¥5000，手牌上限减一"); content.overflow(room); return true;
        }
        if(card.kind.equals("timeRestore")) {
            Tile target=tile(room,action.tileId); require(restorable(room,actor,target),"请选择修复期限内、产权未变化且实际受损的自家地产");
            ContentService.consume(room,actor,card,true,action); target.level=target.damageLevel; target.damageOwnerId=null; log(room,target.name+" 经四之弹恢复至 "+target.level+" 级"); return true;
        }
        if(!attacks(card.kind)) return false;
        attack(room,actor,card,action,true); return true;
    }
    /** 输入已确认主动牌或蓄力释放，固定攻击分段；释放不重复消费卡或主动次数。 */
    public void attack(Room room,Player actor,Card card,Action action,boolean consume) {
        require(CrossoverService.attackAllowed(actor),"王的压制期间不能发动主动攻击");
        require(!consume || CrossoverService.handAllowed(room,actor,card,true),"该牌当前被禁用");
        require(CrossoverService.turns(actor,"DEFENSE_SPECIALIZATION")==0,"防御特化期间不能主动攻击");
        AnimeState.Attack frame=new AnimeState.Attack(); frame.actorId=actor.id; frame.returnPhase=room.phase;
        plan(room,actor,card,action,frame.parts);
        require(!frame.parts.isEmpty(),"当前没有合法目标，此卡和主动次数保留");
        if(card.kind.equals("fireBurial")) {
            Tile sacrifice=tile(room,action.secondTileId);
            require(sacrifice!=null && Objects.equals(sacrifice.ownerId,actor.id) && sacrifice.level>0 && !sacrifice.mortgaged && !PropertyEffects.frozen(sacrifice),"请选择自家未抵押、未封印且有建筑的献祭地产");
            sacrifice.level--; sacrifice.damageOwnerId=null; sacrifice.damageLevel=0; CrossoverService.state(room).damage.remove(sacrifice.id);
            log(room,actor.name+" 献祭 "+sacrifice.name+" 的一级建筑，无退款且不可用四之弹恢复");
        }
        if(card.kind.equals("swordRain")) {
            require(card.lastUsedTurn!=room.turnNumber,"剑雨本回合仍在冷却"); card.remainingUses=(card.remainingUses==0?2:card.remainingUses)-1; card.usesSpent++; card.lastUsedTurn=room.turnNumber;
        }
        if(consume) ContentService.consume(room,actor,card,true,action); if(card.kind.equals("swordRain") && card.remainingUses>0) actor.hand.add(card);
        frame.card=copy(card);
        Set<String> targets=new LinkedHashSet<>();
        for(GameEffect part:frame.parts) if(part.targetId!=null && !part.targetId.equals(actor.id)) targets.add(part.targetId);
        frame.singleTarget=targets.size()==1 && (!card.kind.equals("kamui") || frame.parts.size()==1) && !Set.of("handDestruction","gravity","meteor","sevenSwords","railgun","explosion","absoluteZero","voidTears","swapAny","asunaFlash","shadowBlade").contains(card.kind);
        frame.defenders.addAll(targets); state(room).attack=frame; offerDefense(room);
    }
    /** 输入恢复或移出后的房间，继续没有待选的未完成响应，不重复已入队结果。 */
    public void resume(Room room) {
        if(room.pending!=null || room.runtime.choice!=null || room.trade!=null) return;
        AnimeState state=state(room);
        if(state.attack!=null && !state.attack.queued) { Player actor=player(room,state.attack.actorId); if(actor==null || actor.bankrupt) state.attack=null; else offerDefense(room); }
        if(room.pending==null && !state.voters.isEmpty()) offerVote(room);
    }
    /** 输入房间，返回是否有移出或恢复后需要继续的扩展响应。 */
    public static boolean needsResume(Room room) { AnimeState state=state(room); return room.pending==null && (state.attack!=null && !state.attack.queued || !state.voters.isEmpty()); }
    /** 输入来源和已验证参数，固定所有目标、随机样本与分段，返回空。 */
    private void plan(Room room,Player actor,Card card,Action action,List<GameEffect> parts) {
        if(content.crossover.plan(room,actor,card,action,parts)) return;
        Tile target=tile(room,action.tileId); Player other=player(room,action.playerId); String kind=card.kind;
        switch(kind) {
            case "senbonzakura" -> {
                require(action.tileIds!=null && !action.tileIds.isEmpty() && action.tileIds.size()<=3 && new HashSet<>(action.tileIds).size()==action.tileIds.size(),"请选择不同花瓣目标");
                for(int id:action.tileIds) { Tile t=tile(room,id); require(enemyBuilding(room,actor,t),"花瓣目标须为对手有建筑的未封印地产"); part(parts,room,actor,player(room,t.ownerId),t,"lower",1); }
            }
            case "igniteSea" -> {
                require(enemyBuilding(room,actor,target),"请选择对手有建筑的未封印地产");
                part(parts,room,actor,player(room,target.ownerId),target,"igniteSea",0);
            }
            case "keqingRush","thunderLord","fireBurial" -> {
                require(enemyBuilding(room,actor,target),"请选择对手有建筑的未封印地产");
                if(kind.equals("keqingRush")) require(nearbyDistance(actor.position,target.id,room.board.size())<=6,"请选择前后六格内的对手地产");
                part(parts,room,actor,player(room,target.ownerId),target,kind.equals("thunderLord")?"thunderMark":kind.equals("fireBurial")?"lowerPiercing":"lower",kind.equals("fireBurial")?2:1);
                if(kind.equals("keqingRush")) part(parts,room,actor,actor,target,"rushTeleport",0);
            }
            case "demolish","fullDemolish","starburst","corruption" -> {
                require(enemyBuilding(room,actor,target),"请选择对手有建筑的未封印地产");
                part(parts,room,actor,player(room,target.ownerId),target,kind.equals("corruption")?"corrupt":kind.equals("fullDemolish")?"fullPiercing":"lower",kind.equals("fullDemolish")?3:kind.equals("starburst")?2:1);
            }
            case "barrier" -> {
                require(target!=null && !PropertyEffects.frozen(target) && !target.barrier && nearbyDistance(actor.position,target.id,room.board.size())>=1 && nearbyDistance(actor.position,target.id,room.board.size())<=6,"请选择前后六格内无路障的地块，不含当前格");
                part(parts,room,actor,player(room,target.ownerId),target,"barrier",1);
            }
            case "kamui" -> {
                for(Tile property:kamuiTargets(room,action)) part(parts,room,actor,player(room,property.ownerId),property,"kamui",0);
            }
            case "exchange","blackhole" -> {
                require(target!=null && target.type.equals("property") && !PropertyEffects.frozen(target),"请选择未封印地产");
                if(kind.equals("exchange")) require(target.ownerId!=null && !Objects.equals(target.ownerId,actor.id) && actor.cash>=PropertyEffects.exchangePrice(room,target),"请选择现金足以支付交换价的对手地产，最低为地价");
                if(kind.equals("blackhole")) require(CrossoverService.attached(room,target),"地产没有可清除的附加效果");
                part(parts,room,actor,player(room,target.ownerId),target,kind,kind.equals("exchange")?PropertyEffects.exchangePrice(room,target):0);
            }
            case "swap","blast","jail","steal","sap","slow","reaper","commandSpell","swordRain" -> {
                require(other!=null && !other.bankrupt && (Set.of("blast","jail").contains(kind) || other!=actor),"请选择合法未破产玩家");
                if(kind.equals("swap")) require(!other.jailed,"不能与服刑玩家换位");
                if(kind.equals("sap")) require(nearbyDistance(actor.position,other.position,room.board.size())<=3,"闷棍限前后 3 格内的玩家，含同格");
                if(kind.equals("swordRain")) require(nearbyDistance(actor.position,other.position,room.board.size())<=6,"剑雨限前后六格玩家");
                if(kind.equals("steal")) require(!other.hand.isEmpty(),"对手没有手牌，卡和次数保留");
                if(kind.equals("reaper")) require(PropertyEffects.handLimit(actor)>1 && PropertyEffects.handLimit(other)>1,"双方手牌上限须大于一张");
                if(kind.equals("commandSpell")) require(action.point!=null && action.point>=1 && action.point<=6,"请选择1～6点");
                if(kind.equals("swap")) { part(parts,room,actor,actor,null,"teleport",other.position); part(parts,room,actor,other,null,"teleport",actor.position); }
                else if(kind.equals("blast")) part(parts,room,actor,other,null,"blast",random.nextInt(room.board.size()));
                else if(kind.equals("reaper")) { part(parts,room,actor,actor,null,"handLimit",1); part(parts,room,actor,other,null,"handLimit",1); }
                else if(kind.equals("sap")) { part(parts,room,actor,other,null,"rest",1); part(parts,room,actor,other,null,"steal",1); }
                else part(parts,room,actor,other,null,switch(kind){case "swordRain"->"rest";case "commandSpell"->"command";default->kind;},kind.equals("commandSpell")?action.point:1);
            }
            case "swapAny" -> {
                Player second=player(room,action.secondPlayerId); require(other!=null && second!=null && other!=second && !other.bankrupt && !second.bankrupt,"请选择两名不同的未破产玩家");
                part(parts,room,actor,other,null,"teleport",second.position); part(parts,room,actor,second,null,"teleport",other.position);
            }
            case "handDestruction","gravity" -> {
                for(Player p:room.players) if(!p.bankrupt && (kind.equals("handDestruction") || p!=actor)) part(parts,room,actor,p,null,kind.equals("gravity")?"gather":"redraw",actor.position);
            }
            case "meteor" -> {
                List<Tile> candidates=new ArrayList<>(room.board.stream().filter(t -> t.type.equals("property") && !PropertyEffects.frozen(t)).toList());
                int count=candidates.isEmpty()?0:Math.min(random.nextInt(3)+1,candidates.size());
                for(int i=0;i<count;i++) { Tile t=candidates.remove(random.nextInt(candidates.size())); part(parts,room,actor,player(room,t.ownerId),t,"meteor",1); }
            }
            case "sevenSwords" -> {
                require(action.tileIds!=null && !action.tileIds.isEmpty() && action.tileIds.size()<=3,"请分配最多三道剑气");
                Map<Integer,Long> counts=new LinkedHashMap<>(); action.tileIds.forEach(id -> counts.merge(id,1L,Long::sum));
                for(var entry:counts.entrySet()) { Tile t=tile(room,entry.getKey()); require(enemyBuilding(room,actor,t),"剑气只能分给对手有建筑地产"); part(parts,room,actor,player(room,t.ownerId),t,"lower",entry.getValue()); }
            }
            case "railgun" -> {
                require(action.direction!=null && Math.abs(action.direction)==1,"请选择顺时针或逆时针");
                for(int i=1;i<=6;i++) { Tile t=room.board.get(Math.floorMod(actor.position+action.direction*i,room.board.size())); if(!PropertyEffects.frozen(t)) part(parts,room,actor,player(room,t.ownerId),t,"railgun",Objects.equals(t.ownerId,actor.id)?0:1); }
            }
            case "explosion" -> {
                require(target!=null,"请选择范围起点");
                for(int i=0;i<3;i++) { Tile t=room.board.get((target.id+i)%room.board.size()); if(t.type.equals("property") && t.level>0 && !PropertyEffects.frozen(t)) part(parts,room,actor,player(room,t.ownerId),t,"full",3); }
                require(!parts.isEmpty(),"范围内没有可破坏建筑，此卡保留"); part(parts,room,actor,actor,null,"rest",1);
            }
            case "voidTears" -> {
                Tile second=tile(room,action.secondTileId); require(target!=null && second!=null && target!=second && target.type.equals("property") && second.type.equals("property") && !PropertyEffects.frozen(target) && !PropertyEffects.frozen(second),"请选择两个不同的未封印普通地产");

                GameEffect first=part(parts,room,actor,player(room,target.ownerId),target,"portal",second.id); first.destination=second.id;
                GameEffect last=part(parts,room,actor,player(room,second.ownerId),second,"portal",target.id); last.destination=target.id;
            }
            case "absoluteZero" -> {
                require(target!=null,"请选择棋盘的一条边"); int side=side(room,target.id);
                for(Tile t:room.board) if(t.type.equals("property") && !PropertyEffects.frozen(t) && side(room,t.id)==side) part(parts,room,actor,player(room,t.ownerId),t,"zero",actor.personalTurns+1);
            }
            default -> throw new GameError("主动机制尚未注册");
        }
    }
    /** 输入棋盘和格号，按实际矩形四边返回边编号，角点归后一边。 */
    public static int side(Room room,int id) {
        return boardSide(room.board.size(),id);
    }
    /** 输入棋盘总格数及格号，返回矩形的实际边编号。 */
    public static int boardSide(int size,int id) {
        int horizontal=(size+3)/4, vertical=size/4;
        return id<horizontal?0:id<horizontal+vertical?1:id<2*horizontal+vertical?2:3;
    }
    /** 输入房间及封印命令，返回同一条边的1～3处不同合法地产；兼容旧客户端单个tileId，不限制归属或连续性。 */
    public static List<Tile> kamuiTargets(Room room,Action action) {
        List<Integer> ids=action.tileIds!=null?action.tileIds:action.tileId==null?List.of():List.of(action.tileId);
        require(!ids.isEmpty() && ids.size()<=3 && ids.stream().allMatch(Objects::nonNull) && new HashSet<>(ids).size()==ids.size(),"请选择1～3处不同的地产");
        List<Tile> targets=ids.stream().map(id -> tile(room,id)).toList();
        require(targets.stream().allMatch(t -> t!=null && t.type.equals("property") && !PropertyEffects.frozen(t)),"请选择未封印的普通地产");
        int selectedSide=side(room,targets.getFirst().id);
        require(targets.stream().allMatch(t -> side(room,t.id)==selectedSide),"封印目标须在同一条边，无需连续，可混选自己和对手的地产");
        return targets;
    }
    /** 输入目标地产与施术者，返回是否是仍有建筑的合法敌方地产。 */
    private static boolean enemyBuilding(Room room,Player actor,Tile tile) { Player owner=tile==null?null:player(room,tile.ownerId); return tile!=null && tile.type.equals("property") && !PropertyEffects.frozen(tile) && tile.level>0 && owner!=null && !owner.bankrupt && owner!=actor; }
    /** 输入固定分段参数，保存已解析接收者、地产与数值，返回该分段。 */
    private GameEffect part(List<GameEffect> parts,Room room,Player actor,Player target,Tile tile,String kind,long amount) {
        GameEffect e=effects.effect("ANIME_PART",actor); e.targetId=target==null?null:target.id; e.tileId=tile==null?null:tile.id; e.propertyTarget=tile!=null; e.selector=kind; e.amount=amount; parts.add(e); return e;
    }
    /** 输入当前攻击，逐个公开响应身份，候选卡仅投影给本人。 */
    private void offerDefense(Room room) {
        AnimeState.Attack frame=state(room).attack;
        while(frame.nextDefender<frame.defenders.size()) {
            Player owner=player(room,frame.defenders.get(frame.nextDefender++)); if(owner==null || owner.bankrupt) continue;
            boolean playerTarget=frame.parts.stream().anyMatch(e -> Objects.equals(e.targetId,owner.id) && !e.propertyTarget);
            boolean control=frame.parts.stream().anyMatch(e -> Objects.equals(e.targetId,owner.id) && controlledPart(e));
            if(CrossoverService.turns(owner,"DEFENSE_SPECIALIZATION")>0) { frame.defenses.put(owner.id,"imagineBreaker"); continue; }
            List<Card> cards=owner.hand.stream().filter(c -> CrossoverService.handAllowed(room,owner,c,false)).filter(c -> c.kind.equals("imagineBreaker") || c.kind.equals("vectorReflect") && playerTarget || c.kind.equals("sealCard") && frame.singleTarget || c.kind.equals("hollowfication") && control).toList();
            if(cards.isEmpty()) continue;
            ContentChoice choice=content.open(room,owner,"anime_defense"); choice.cards.addAll(cards); choice.content=frame.card; choice.prompt=player(room,frame.actorId).name+" 使用「"+frame.card.name+"」，可选择防御；虚化仅抵消针对本人的强制位移、休息和监禁。"; return;
        }
        finishDefense(room);
    }
    /** 输入已完成响应的攻击，按本人部分取消或反射，固定后入队，不重新抽样。 */
    private void finishDefense(Room room) {
        AnimeState.Attack frame=state(room).attack; frame.queued=true;
        boolean portalBlocked=frame.card.kind.equals("voidTears") && frame.defenses.values().stream().anyMatch(v -> !v.isEmpty());
        boolean swapBlocked=Set.of("swap","swapAny").contains(frame.card.kind) && frame.defenses.values().stream().anyMatch("hollowfication"::equals);
        for(GameEffect e:frame.parts) {
            String defense=frame.defenses.getOrDefault(e.targetId,"");
            Player recipient=player(room,e.targetId);
            if(recipient!=null && !recipient.id.equals(frame.actorId) && CrossoverService.turns(recipient,"DEFENSE_SPECIALIZATION")>0) continue;
            if(swapBlocked || defense.equals("hollowfication") && controlledPart(e)) continue;
            if(portalBlocked || defense.equals("imagineBreaker") || defense.equals("sealCard")) continue;
            if(defense.equals("vectorReflect") && !e.propertyTarget) {
                String defender=e.targetId; e.targetId=frame.actorId; e.playerId=defender;
                if(Set.of("teleport","gather").contains(e.selector)) e.amount=player(room,defender).position;
            }
            e.content=frame.card; effects.submit(room,e);
        }
        room.phase=frame.returnPhase;
        effects.submit(room,effects.effect("ANIME_FINISH",player(room,frame.actorId)));
    }
    /** 输入攻击分段，返回是否为虚化可抵消的本人强制控制；自愿突入不在此列。 */
    private static boolean controlledPart(GameEffect effect) { return !effect.propertyTarget && Set.of("teleport","gather","blast","jail","rest","command").contains(effect.selector); }
    /** 输入当前选择和答复，完成重掷、投影、主动防御或秘密投票，返回是否处理。 */
    public boolean decide(Room room,Player owner,ContentChoice choice,Action action) {
        if(!choice.type.startsWith("anime_")) return false;
        require(action.accept!=null,"请选择确认或放弃");
        if(choice.type.equals("anime_projection") && action.accept) {
            Card selected=choice.cards.stream().filter(c -> Objects.equals(c.id,action.cardId)).findFirst().orElseThrow(() -> new GameError("请选择上一回合的合法道具"));
            ContentService.consume(room,owner,ContentService.find(owner,choice.cardId),true,action); Card projected=copy(selected); projected.id=random.id(); projected.projected=true; projected.expiresAtTurn=room.turnNumber; projected.lastUsedTurn=0;
            owner.hand.add(projected); owner.contentNotice=projected; owner.contentNoticeText="投影牌本回合可额外使用一次，回合结束消失";
        } else if(choice.type.equals("anime_rewind") && action.accept) {
            Card card=ContentService.find(owner,action.cardId); require(card.kind.equals("timeRewind"),"请选择时间溯行"); ContentService.consume(room,owner,card,false,action);
            room.dice=random.nextInt(6)+1; room.runtime.movementSteps=room.dice; log(room,owner.name+" 时间溯行后掷出 "+room.dice+" 点，必须接受此次结果");
        } else if(choice.type.equals("anime_defense") && action.accept) {
            Card defense=choice.cards.stream().filter(c -> Objects.equals(c.id,action.cardId)).findFirst().orElseThrow(() -> new GameError("请选择本次合法防御牌"));
            ContentService.consume(room,owner,ContentService.find(owner,defense.id),false,action); AnimeState.Attack frame=state(room).attack; frame.defenses.put(owner.id,defense.kind);
            if(defense.kind.equals("sealCard")) { Card captured=copy(frame.card); captured.id=random.id(); if(captured.kind.equals("swordRain")) captured.remainingUses=Math.max(1,captured.remainingUses); owner.hand.add(captured); owner.contentNotice=captured; owner.contentNoticeText="已封印并收服这张道具"; }
            log(room,owner.name+" 使用「"+defense.name+"」响应「"+frame.card.name+"」");
        } else if(choice.type.equals("anime_vote")) {
            String vote=action.accept?action.playerId:""; require(vote!=null && (vote.isEmpty() || choice.playerIds.contains(vote)),"请选择其他玩家或弃权"); state(room).votes.put(owner.id,vote);
        }
        room.pending=choice.returnPending; room.phase=choice.returnPhase; room.runtime.choice=null;
        if(choice.type.equals("anime_defense")) offerDefense(room);
        if(choice.type.equals("anime_vote")) offerVote(room);
        return true;
    }
    /** 输入抽到学级裁判的玩家，固定参与者并开始秘密投票。 */
    public void trial(Room room,Player actor,ContentResult result) {
        AnimeState state=state(room); state.voters=new ArrayList<>(room.players.stream().filter(p -> !p.bankrupt).map(p -> p.id).toList()); state.votes.clear(); offerVote(room);
    }
    /** 输入已提交票，逐位等待后一次揭晓，最高票并列仅抽样一次。 */
    private void offerVote(Room room) {
        AnimeState state=state(room);
        for(String id:state.voters) {
            Player owner=player(room,id); if(owner==null || owner.bankrupt || state.votes.containsKey(id)) continue;
            ContentChoice choice=content.open(room,owner,"anime_vote"); choice.playerIds.addAll(state.voters.stream().filter(v -> !v.equals(id) && player(room,v)!=null && !player(room,v).bankrupt).toList()); choice.prompt="秘密投给一名其他玩家，或选择弃权；提交前不公开票数。"; return;
        }
        Map<String,Integer> counts=new LinkedHashMap<>(); for(String id:state.votes.values()) if(!id.isEmpty() && player(room,id)!=null && !player(room,id).bankrupt) counts.merge(id,1,Integer::sum);
        log(room,"学级裁判投票揭晓："+state.voters.stream().map(id -> player(room,id).name+"→"+(state.votes.getOrDefault(id,"").isEmpty()?"弃权":player(room,state.votes.get(id)).name)).toList());
        state.voters.clear(); state.votes.clear();
        if(!counts.isEmpty()) { int max=Collections.max(counts.values()); List<String> tied=counts.keySet().stream().filter(id -> counts.get(id)==max).toList(); Player target=player(room,tied.get(random.nextInt(tied.size()))); content.restrict(room,target,1,false); }
    }
    /** 输入付款人及回归牌，校验真实清算条件后仅取消当前余额，返回空。 */
    public void deathReturn(Room room,Player owner,Card card,Action action) {
        require(card.kind.equals("deathReturn") && room.pending!=null && room.pending.defenseResolved && paymentPending(room) && room.pending.playerId.equals(owner.id),"当前没有可回归的清算费用");
        long value=owner.cash; for(Tile t:room.board) if(Objects.equals(t.ownerId,owner.id) && !PropertyEffects.frozen(t)) value=Math.addExact(value,Rules.propertyLiquidationValue(room,t));
        require(value<room.pending.amount,"尚未达到破产清算条件");
        require(room.pending.creditorId!=null || room.board.stream().noneMatch(t -> Objects.equals(t.ownerId,owner.id) && !t.mortgaged && !PropertyEffects.frozen(t)),"系统费用须先处理全部可抵押地产");
        ContentService.consume(room,owner,card,false,action); room.pending=null; room.phase="manage"; relocate(room,owner,owner.turnStartPosition,"deathReturn"); owner.cash=Math.addExact(owner.cash,3000); content.restrict(room,owner,1,false,false); log(room,owner.name+" 死亡回归，当前剩余债务取消，获得 ¥3000");
    }
    /** 输入固定分段，执行已完成主动防御的结果；等待解控时后续分段留在原队列。 */
    private void resolve(Room room,GameEffect e) {
        Player actor=player(room,e.playerId), target=player(room,e.targetId); Tile tile=tile(room,e.tileId);
        if(actor==null || actor.bankrupt || target!=null && target.bankrupt) return;
        switch(e.selector) {
            case "igniteSea" -> {
                if(tile!=null && !PropertyEffects.frozen(tile) && tile.level>0) {
                    int shields=PropertyEffects.shieldCharges(tile); tile.demolitionShield=false; tile.demolitionShieldCharges=0;
                    GameEffect damage=effects.effect("NETA_DAMAGE",actor); damage.tileId=tile.id; damage.amount=Math.min(3,1+shields); damage.content=e.content; effects.submit(room,damage);
                }
            }
            case "thunderMark" -> NetaService.mark(room,actor,tile);
            case "lowerPiercing" -> { if(tile!=null && !PropertyEffects.frozen(tile) && tile.level>0) { recordDamage(room,tile); tile.level=Math.max(0,tile.level-(int)e.amount); } }
            case "rushTeleport" -> { GameEffect move=effects.effect("DIRECTED_TELEPORT",actor); move.tileId=tile.id; effects.submit(room,move); }
            case "lower" -> lower(room,tile,(int)e.amount);
            case "full","fullPiercing" -> { if(tile!=null && !PropertyEffects.frozen(tile) && tile.level>0 && (e.selector.equals("fullPiercing") || !PropertyEffects.preventDemolition(room,tile))) { recordDamage(room,tile); tile.level=0; log(room,tile.name+" 的建筑被全部摧毁"+(e.selector.equals("fullPiercing")?"，无视护盾":"")); } }
            case "corrupt" -> { if(tile!=null && !PropertyEffects.frozen(tile)) { tile.corruptionTurns=3; log(room,tile.name+" 受到猩红腐败，剩余三次侵蚀"); } }
            case "railgun" -> { if(tile!=null && !PropertyEffects.frozen(tile)) { tile.barrier=false; tile.meteorCharges=0; tile.demolitionShield=false; tile.demolitionShieldCharges=0; if(e.amount>0) lower(room,tile,1); } }
            case "barrier" -> tile.barrier=true;
            case "meteor" -> tile.meteorCharges=Math.addExact(tile.meteorCharges,1);
            case "kamui" -> { tile.kamuiCasterId=actor.id; tile.kamuiTurns=3; tile.kamuiAppliedTurn=room.turnNumber; }
            case "zero" -> { tile.zeroCasterId=actor.id; tile.zeroUntilTurn=(int)e.amount; }
            case "portal" -> { if(tile.portalTileId!=null) { Tile old=tile(room,tile.portalTileId); if(old!=null && Objects.equals(old.portalTileId,tile.id)) old.portalTileId=null; } tile.portalTileId=e.destination; }
            case "blackhole" -> CrossoverService.clear(room,tile,false);
            case "exchange" -> { if(actor.cash<e.amount) { log(room,"现金已变化，等价交换未执行"); break; } actor.cash-=e.amount; if(target!=null) target.cash=Math.addExact(target.cash,e.amount); tile.ownerId=actor.id; observeOwnership(room); }
            case "teleport","gather" -> relocate(room,target,(int)e.amount,"teleport");
            case "blast" -> { relocate(room,target,(int)e.amount,"teleport"); effects.submit(room,effects.effect("SETTLE_TILE",target)); }
            case "jail" -> { GameEffect jail=effects.effect("SEND_TO_JAIL",target); jail.voluntaryRestriction=actor==target; jail.content=e.content; effects.submit(room,jail); }
            case "rest" -> content.restrict(room,target,1,false,actor!=target);
            case "slow" -> { GameEffect slow=effects.effect("ADD_STATUS",target); slow.statusId="NEXT_ROLL_HALF"; slow.amount=1; effects.submit(room,slow); }
            case "handLimit" -> { target.handLimitReduction++; content.overflow(room); }
            case "steal" -> {
                if(!target.hand.isEmpty()) { Card stolen=target.hand.remove(random.nextInt(target.hand.size())); actor.hand.add(stolen); actor.contentNotice=stolen; actor.contentNoticeText="获得转移的私人道具"; target.contentNotice=stolen; target.contentNoticeText="这张道具被转移给 "+actor.name; content.overflow(room); }
            }
            case "redraw" -> { int count=target.hand.size(); target.hand.clear(); content.giveCards(room,target,count,null); content.overflow(room); }
            case "command" -> {
                GameEffect move=effects.effect("MOVE",target); move.amount=e.amount; move.content=e.content; effects.submit(room,move); GameEffect land=effects.effect("SETTLE_TILE",target); land.content=e.content; effects.submit(room,land);
            }
            default -> content.crossover.resolvePart(room,e);
        }
        log(room,"「"+(e.content==null?e.selector:e.content.name)+"」效果已结算"+(tile==null?target==null?"":"："+target.name:"："+tile.name));
    }
    /** 输入建筑分段的次数，每一击独立护盾并记录实际损坏，不附加灾害补偿。 */
    private static void lower(Room room,Tile tile,int hits) {
        if(tile==null || PropertyEffects.frozen(tile)) return;
        boolean recorded=false;
        for(int i=0;i<hits && tile.level>0;i++) if(!PropertyEffects.preventDemolition(room,tile)) { if(!recorded) { recordDamage(room,tile); recorded=true; } tile.level--; log(room,tile.name+" 的建筑降低一级，现为 "+tile.level+" 级"); }
    }
    /** 输入玩家及已固定目的地，只记录直接传送，不产生路径、奖励或落点。 */
    private static void relocate(Room room,Player target,int destination,String kind) {
        if(target==null || target.bankrupt) return; MotionSegment segment=BoardEffect.begin(kind,target); BoardEffect.movement(segment,target,destination); target.position=destination; BoardEffect.publish(room,segment);
    }
}
