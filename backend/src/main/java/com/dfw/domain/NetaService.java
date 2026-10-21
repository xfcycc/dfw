package com.dfw.domain;

import com.dfw.domain.rules.ContentCatalog;
import java.util.*;
import static com.dfw.domain.GameError.require;
import static com.dfw.domain.GameSupport.*;

/** 八素材的资源交换、有限连携、公共研究和攻防共享花瓣。 */
public final class NetaService {
    private final ContentService content;
    private final EffectResolver effects;
    private final RandomSource random;
    private final ContentCatalog catalog;
    /** 输入现有内容、效果队列和随机目录，复用事务及私人选择。 */
    public NetaService(ContentService content, EffectResolver effects, RandomSource random, ContentCatalog catalog) {
        this.content=content; this.effects=effects; this.random=random; this.catalog=catalog;
        effects.beforeEffect(this::offerPetalDefense);
        effects.register("NETA_DAMAGE",(room,effect) -> {
            Tile target=tile(room,effect.tileId); if(target==null || PropertyEffects.frozen(target)) return;
            boolean recorded=false;
            for(int i=0;i<effect.amount && target.level>0;i++) if(!PropertyEffects.preventDemolition(room,target)) {
                if(!recorded) { AnimeService.recordDamage(room,target); recorded=true; } target.level--;
            }
            if("corruption".equals(effect.selector)) { target.corruptionTurns--; if(target.level==0) target.corruptionTurns=0; }
            log(room,target.name+" 追击结算，剩余建筑 "+target.level+" 级");
        });
    }
    /** 输入房间，返回兼容旧快照的扩展状态；未开局大厅只返回空状态，不创建对局运行时。 */
    public static NetaState state(Room room) { if(room.runtime==null) return new NetaState(); if(room.runtime.neta==null) room.runtime.neta=new NetaState(); return room.runtime.neta; }
    /** 输入房间，清除转手、淘汰或已到期的标记，不改变资产等级。 */
    public static void clean(Room room) {
        if(room.runtime==null) return;
        state(room).thunder.entrySet().removeIf(entry -> {
            Player caster=player(room,entry.getKey()); Tile t=tile(room,entry.getValue().tileId);
            return caster==null || caster.bankrupt || t==null || !Objects.equals(t.ownerId,entry.getValue().ownerId);
        });
        state(room).petals.entrySet().removeIf(entry -> { Player p=player(room,entry.getKey()); return p==null || p.bankrupt || p.personalTurns>entry.getValue().untilTurn; });
        state(room).defenses.keySet().removeIf(id -> room.runtime.effects.stream().noneMatch(e -> Objects.equals(e.id,id)));
        for(Tile t:room.board) if(t.petalCasterId!=null) {
            Player caster=player(room,t.petalCasterId);
            if(caster==null || caster.bankrupt || caster.personalTurns>=t.petalUntilTurn || !Objects.equals(t.ownerId,t.petalOwnerId)) clearPetal(t);
        }
    }
    /** 输入地产和当前房间，清除该地产所有本批附加效果，供黑洞调用。 */
    public static void clear(Room room,Tile tile) { state(room).thunder.values().removeIf(mark -> mark.tileId==tile.id); clearPetal(tile); }
    /** 输入地产，清除一次性花瓣租金标记，无返回值。 */
    private static void clearPetal(Tile tile) { tile.petalCasterId=null; tile.petalOwnerId=null; tile.petalUntilTurn=0; }
    /** 输入已消费主动次数的玩家，为其他施术者各贡献至多一击。 */
    public static void played(Room room,Player owner) {
        clean(room);
        state(room).thunder.forEach((caster,mark) -> {
            if(!caster.equals(owner.id) && mark.charged.add(owner.id)) {
                mark.hits=Math.min(4,mark.hits+1); log(room,"神君蓄势："+tile(room,mark.tileId).name+"，当前 "+mark.hits+" 击");
            }
        });
    }
    /** 输入施术者回合开始后的房间，先过期花瓣，再执行一次已锁定的神君追击。 */
    public void startTurn(Room room,Player owner) {
        clean(room); NetaState.Thunder mark=state(room).thunder.get(owner.id);
        if(mark==null || owner.personalTurns<mark.untilTurn) return;
        state(room).thunder.remove(owner.id); Tile t=tile(room,mark.tileId);
        if(t==null || PropertyEffects.frozen(t)) { log(room,"神君追击结束：目标处于封印中"); return; }
        GameEffect e=effects.effect("NETA_DAMAGE",owner); e.tileId=t.id; e.targetId=t.ownerId; e.amount=mark.hits;
        Card source=new Card(); source.kind="thunderLord"; source.mechanism=source.kind; source.pool="card"; source.name="神君追击"; e.content=source;
        effects.submit(room,e); log(room,"神君追击 "+t.name+"，斩击 "+mark.hits+" 次");
    }
    /** 输入固定标记分段，建立或替换施术者神君目标，无额外防御窗口。 */
    public static void mark(Room room,Player actor,Tile target) {
        NetaState.Thunder mark=new NetaState.Thunder(); mark.tileId=target.id; mark.ownerId=target.ownerId; mark.untilTurn=actor.personalTurns+1;
        state(room).thunder.put(actor.id,mark); log(room,target.name+" 被神君锁定，初始2击");
    }
    /** 输入实际产生的正租金，在演唱会和付款防御之前消耗花瓣并按低一级计算。 */
    public static long petalRent(Room room,Tile tile,long amount) {
        clean(room);
        if(tile.petalCasterId==null || amount<=0) return amount;
        int level=Math.max(0,tile.level-1); EconomyRules economy=Rules.economy(room);
        long reduced=economy.derived("rent."+tile.price+"."+level,Math.multiplyExact(tile.price,economy.rentBasePercent)/100*Rules.RENT_MULTIPLIERS[level]);
        clearPetal(tile); log(room,tile.name+" 消耗千本樱标记，本笔按 "+level+" 级基础租金 ¥"+reduced+" 结算"); return reduced;
    }
    /** 输入普通道具；双骰确认发动立即消费并固定样本，后续只能选择路线。 */
    public boolean use(Room room,Player owner,Card card,Action action) {
        if(modern(card) && card.kind.equals("senbonzakura")) {
            ContentService.consume(room,owner,card,true,action);
            NetaState.Petals petals=state(room).petals.get(owner.id);
            if(petals==null) { petals=new NetaState.Petals(); petals.untilTurn=owner.personalTurns+3; state(room).petals.put(owner.id,petals); }
            petals.charges=3; petals.source=AnimeService.copy(card); log(room,owner.name+" 展开千本樱，花瓣补至3片，剩余期限不延长"); return true;
        }
        if(!card.kind.equals("bankai")) return false;
        if(modern(card)) {
            ContentService.consume(room,owner,card,true,action); NetaState state=state(room);
            state.bankaiTurn=room.turnNumber; state.bankaiPlayerId=owner.id; state.bankaiRemaining=2;
            log(room,owner.name+" 卍解：本回合最多再使用2张其他主动道具"); return true;
        }
        ContentService.consume(room,owner,card,true,action);
        ContentChoice choice=content.open(room,owner,"neta_bankai"); choice.content=card;
        choice.numbers=List.of(random.nextInt(6)+1,random.nextInt(6)+1); choice.prompt="骰点已固定，请选择一枚骰子和前进／后退方向；不能取消重掷。"; return true;
    }
    /** 输入本批事件，固定资源、候选或地图标记，不增加事件抽取次数。 */
    public void event(Room room,Player owner,ContentResult source) {
        switch(source.mechanism) {
            case "igniteSea" -> {
                if(owner.hand.isEmpty()) { log(room,"点燃大海：没有可用燃料，本次结束"); return; }
                ContentChoice choice=content.open(room,owner,"neta_ignite"); choice.content=source; choice.cards.addAll(owner.hand); choice.prompt="可弃置一张手牌，换取前进1～6格；也可放弃。";
            }
            case "geniusClub" -> {
                NetaState.Research research=new NetaState.Research(); research.source=source;
                research.triggerId=owner.id; research.cards=(modern(source)?catalog.researchUr(room,random):catalog.research(room,random)).stream().map(ContentService::card).toList();
                research.participants=room.players.stream().filter(p -> !p.bankrupt).map(p -> p.id).toList();
                state(room).research=research; resume(room);
            }
            case "senbonzakura" -> {
                clean(room); List<Tile> candidates=new ArrayList<>(room.board.stream().filter(t -> t.type.equals("property") && t.level>0 && t.ownerId!=null && !t.mortgaged && !PropertyEffects.frozen(t) && t.petalCasterId==null && !player(room,t.ownerId).bankrupt).toList());
                int count=Math.min(3,candidates.size());
                for(int i=0;i<count;i++) { Tile t=candidates.remove(random.nextInt(candidates.size())); t.petalCasterId=owner.id; t.petalOwnerId=t.ownerId; t.petalUntilTurn=owner.personalTurns+1; log(room,t.name+" 获得千本樱标记，下次正租金按低一级计算"); }
                if(count==0) log(room,"千本樱：没有合法地产，本次结束");
            }
            default -> throw new GameError("未知八素材事件");
        }
    }
    /** 输入研究中的房间，跳过已淘汰者，顺序收集秘密选择后统一抽签发放。 */
    public void resume(Room room) {
        clean(room); NetaState.Research research=state(room).research;
        if(research==null || room.pending!=null || room.runtime.choice!=null || room.trade!=null) return;
        if(modern(research.source)) { resumeResearch(room,research); return; }
        for(String id:research.participants) {
            Player owner=player(room,id); if(owner==null || owner.bankrupt || research.selections.containsKey(id)) continue;
            ContentChoice choice=content.open(room,owner,"neta_research"); choice.cards.addAll(research.cards); choice.content=research.source;
            choice.prompt="秘密选择一项公共成果或放弃；同项冲突随机一人获得，不公开选择与获牌对应关系。"; return;
        }
        for(Card candidate:research.cards) {
            List<Player> contenders=research.participants.stream().map(id -> player(room,id)).filter(p -> p!=null && !p.bankrupt && candidate.id.equals(research.selections.get(p.id))).toList();
            if(contenders.isEmpty()) continue;
            Player winner=contenders.get(random.nextInt(contenders.size())); Card awarded=AnimeService.copy(candidate); awarded.id=random.id(); winner.hand.add(awarded);
            winner.contentNotice=awarded; winner.contentNoticeText="获得天才俱乐部研究成果"; log(room,winner.name+" 获得一份研究成果");
        }
        state(room).research=null; content.overflow(room);
    }
    /** 输入本人选择答复；先复核固定候选，再恢复父队列，返回是否属于本批窗口。 */
    public boolean decide(Room room,Player owner,ContentChoice choice,Action action) {
        if(!choice.type.startsWith("neta_")) return false;
        require(action.accept!=null,"请选择确认或放弃");
        if(choice.type.equals("neta_bid")) {
            NetaState.Research research=state(room).research;
            require(research!=null && !research.bids.containsKey(owner.id),"研究竞标已失效");
            List<String> ids=Boolean.TRUE.equals(action.accept) && action.discardCardIds!=null?action.discardCardIds:List.of();
            require(ids.size()<=2 && new HashSet<>(ids).size()==ids.size() && ids.stream().allMatch(id -> choice.cards.stream().anyMatch(c -> c.id.equals(id)) && owner.hand.stream().anyMatch(c -> c.id.equals(id))),"最多投入两张本人手牌");
            research.bids.put(owner.id,action.accept?new ArrayList<>(ids):null);
        } else if(choice.type.equals("neta_reward")) {
            NetaState.Research research=state(room).research;
            require(action.accept && research!=null && owner.id.equals(research.winnerId),"请确认本次研究奖励");
            Card selected=research.cards.stream().filter(c -> c.id.equals(action.cardId)).findFirst().orElseThrow(() -> new GameError("请选择一张UR研究成果"));
            List<String> bid=research.bids.get(owner.id);
            require(bid!=null && bid.stream().allMatch(id -> owner.hand.stream().anyMatch(c -> c.id.equals(id))),"竞标材料已变化");
            owner.hand.removeIf(c -> bid.contains(c.id)); Card awarded=AnimeService.copy(selected); awarded.id=random.id(); owner.hand.add(awarded);
            owner.contentNotice=awarded; owner.contentNoticeText="获得天才俱乐部研究成果";
            log(room,owner.name+" 获得1张UR研究成果，投入手牌不折现"); state(room).research=null;
        } else if(choice.type.equals("neta_petals")) {
            if(action.accept) {
                NetaState.Petals petals=state(room).petals.get(owner.id); List<Integer> ids=action.tileIds;
                require(petals!=null && ids!=null && !ids.isEmpty() && ids.size()<=petals.charges && new HashSet<>(ids).size()==ids.size() && ids.stream().allMatch(choice.tileIds::contains),"请选择花瓣数量以内的不同对手地产");
                require(petalsAvailable(room,owner),"本回合花瓣已释放或受到主动攻击限制");
                // 先恢复父窗口，再进入原主动防御链；对手防御不会退回已消耗花瓣。
                room.pending=choice.returnPending; room.phase=choice.returnPhase; room.runtime.choice=null;
                action.tileIds=ids; content.anime.attack(room,owner,petals.source,action,false);
                petals.charges-=ids.size(); petals.lastAttackTurn=room.turnNumber; return true;
            }
        } else if(choice.type.equals("neta_petal_defense")) {
            int count=action.accept && action.selection!=null?action.selection:0; NetaState.Petals petals=state(room).petals.get(owner.id);
            require(count>=0 && count<=(petals==null?0:petals.charges),"花瓣额度已变化");
            state(room).defenses.computeIfAbsent(choice.cardId,k -> new LinkedHashMap<>()).put(owner.id,count);
        } else if(choice.type.equals("neta_bankai")) {
            require(action.accept && choice.numbers.contains(action.point) && action.direction!=null && Math.abs(action.direction)==1,"请选择固定骰点及前进／后退方向");
            room.dice=action.point; room.runtime.movementSteps=action.point; room.runtime.movementDirection=action.direction;
            room.runtime.rollMode="BANKAI"; room.runtime.windowExit="replace_roll"; log(room,owner.name+" 天锁斩月：选择 "+action.point+" 点"+(action.direction==1?"前进":"后退"));
        } else if(choice.type.equals("neta_ignite") && action.accept) {
            require(choice.cards.stream().anyMatch(c -> c.id.equals(action.cardId)) && action.point!=null && action.point>=1 && action.point<=6,"请选择一张燃料牌及1～6步");
            Card fuel=ContentService.find(owner,action.cardId); owner.hand.remove(fuel);
            GameEffect move=effects.effect("MOVE",owner); move.amount=action.point; move.content=choice.content; move.resumesDecision=true;
            GameEffect land=effects.effect("SETTLE_TILE",owner); land.content=choice.content; effects.submitAll(room,List.of(move,land)); log(room,owner.name+" 消耗一张私人手牌作为燃料，前进 "+action.point+" 格");
        } else if(choice.type.equals("neta_research")) {
            NetaState.Research research=state(room).research; require(research!=null && research.participants.contains(owner.id) && !research.selections.containsKey(owner.id),"研究选择已失效");
            require(!action.accept || research.cards.stream().anyMatch(c -> c.id.equals(action.cardId)),"请选择公共研究候选"); research.selections.put(owner.id,action.accept?action.cardId:"");
        }
        room.pending=choice.returnPending; room.phase=choice.returnPhase; room.runtime.choice=null;
        if(Set.of("neta_research","neta_bid","neta_reward").contains(choice.type)) { resume(room); if(state(room).research==null) content.overflow(room); }
        return true;
    }
    /** 输入冻结内容，只有显式第二版参数使用本批最终UR规则。 */
    public static boolean modern(ContentResult card) { return card.parameters.getOrDefault("version",0L)>=2; }
    /** 输入房间及玩家，返回卍解本回合是否已发动，耗尽后也不能借投影绕过。 */
    public static boolean bankaiActive(Room room,Player owner) { NetaState s=state(room); return s.bankaiTurn==room.turnNumber && Objects.equals(s.bankaiPlayerId,owner.id); }
    /** 输入真实卡，统一限制卍解期间所有后续出牌，禁止卍解及投影递归。 */
    public static boolean allowed(Room room,Player owner,Card card) {
        return !bankaiActive(room,owner) || state(room).bankaiRemaining>0 && !Set.of("bankai","projection").contains(card.kind);
    }
    /** 输入成功消费的主动卡，仅后续出牌扣除共享额度，防御和持续技能不算出牌。 */
    public static void spent(Room room,Player owner) { if(bankaiActive(room,owner)) state(room).bankaiRemaining--; }
    /** 输入个人回合结束者，销毁达到第三个后续回合结束的持续花瓣。 */
    public static void endTurn(Room room,Player owner) {
        NetaState.Petals p=state(room).petals.get(owner.id); if(p!=null && owner.personalTurns>=p.untilTurn) state(room).petals.remove(owner.id);
    }
    /** 输入持有人，返回可公开的剩余花瓣与持续期限。 */
    public static List<String> playerLabels(Room room,Player owner) {
        NetaState.Petals p=state(room).petals.get(owner.id);
        return p==null?List.of():List.of("千本樱："+p.charges+"片 · 至第"+p.untilTurn+"个个人回合结束");
    }
    /** 输入持有人，返回是否可在掷骰前使用一次花瓣攻击，不占普通主动次数。 */
    public static boolean petalsAvailable(Room room,Player owner) {
        NetaState.Petals p=state(room).petals.get(owner.id);
        return p!=null && p.charges>0 && p.lastAttackTurn!=room.turnNumber && CrossoverService.handAllowed(room,owner,p.source,true) && CrossoverService.turns(owner,"DEFENSE_SPECIALIZATION")==0;
    }
    /** 输入本人空闲窗口，复用私人选地弹框；无目标时不消耗花瓣。 */
    public void openPetals(Room room,Player owner) {
        require(current(room)==owner && room.phase.equals("pre_roll") && room.pending==null && room.trade==null && petalsAvailable(room,owner),"当前不能释放千本樱");
        List<Integer> targets=room.board.stream().filter(t -> t.type.equals("property") && t.ownerId!=null && !t.ownerId.equals(owner.id) && !player(room,t.ownerId).bankrupt && t.level>0 && !PropertyEffects.frozen(t)).map(t -> t.id).toList();
        require(!targets.isEmpty(),"没有可攻击地产，花瓣保留"); ContentChoice c=content.open(room,owner,"neta_petals");
        c.tileIds=targets; c.content=state(room).petals.get(owner.id).source; c.numbers=List.of(state(room).petals.get(owner.id).charges);
        c.prompt="选择1～"+c.numbers.getFirst()+"处不同对手地产，各消耗1片花瓣降1级；本回合只能释放一次，对手仍可主动防御。";
    }
    /** 输入已锁定研究，先收集秘密投入，再仅抽签一次，胜者自选一张UR。 */
    private void resumeResearch(Room room,NetaState.Research research) {
        for(String id:research.participants) {
            Player p=player(room,id); if(p==null || p.bankrupt || research.bids.containsKey(id)) continue;
            ContentChoice c=content.open(room,p,"neta_bid"); c.cards.addAll(p.hand); c.content=research.source;
            c.prompt="秘密投入0～2张手牌，或放弃。每张1贡献"+(id.equals(research.triggerId)?"，你是触发者，额外＋1":"")+"；只有胜者支付，其他人保留手牌。"; return;
        }
        if(research.winnerId==null || player(room,research.winnerId)==null || player(room,research.winnerId).bankrupt) {
            Map<String,Integer> scores=new LinkedHashMap<>();
            for(String id:research.participants) { Player p=player(room,id); List<String> bid=research.bids.get(id); if(p!=null && !p.bankrupt && bid!=null) scores.put(id,bid.size()+(id.equals(research.triggerId)?1:0)); }
            if(scores.isEmpty()) { state(room).research=null; log(room,"天才俱乐部：所有人放弃，本次结束"); content.overflow(room); return; }
            int max=Collections.max(scores.values()); List<String> tied=scores.keySet().stream().filter(id -> scores.get(id)==max).toList();
            research.winnerId=tied.get(random.nextInt(tied.size()));
            log(room,"天才俱乐部贡献揭晓："+research.participants.stream().map(id -> player(room,id).name+" "+(scores.containsKey(id)?scores.get(id):"放弃")).toList()+"；胜者 "+player(room,research.winnerId).name);
        }
        ContentChoice c=content.open(room,player(room,research.winnerId),"neta_reward"); c.cards.addAll(research.cards); c.content=research.source;
        c.prompt="你已赢得研究成果，请选择1张UR；确认后仅弃置你的竞标材料，不折现。其他人不付代价。";
    }
    /** 输入待执行效果，在常规建筑损坏前收集各业主的可选花瓣额度，穿盾及自愿献祭不开放。 */
    private boolean offerPetalDefense(Room room,GameEffect effect) {
        boolean targeted=effect.type.equals("NETA_DAMAGE") || effect.type.equals("DEMOLISH") || effect.type.equals("ANIME_PART") && Set.of("lower","full").contains(effect.selector);
        boolean destroy=effect.type.equals("DESTROY_BUILDINGS") && !(effect.content!=null && "card".equals(effect.content.pool) && "fullDemolish".equals(effect.content.mechanism));
        boolean all=effect.type.equals("LOWER_ALL_BUILDINGS") || destroy && "RANDOM_BUILT_PROPERTY".equals(effect.selector);
        if(!targeted && !destroy && !all) return false;
        Map<String,Integer> decisions=state(room).defenses.computeIfAbsent(effect.id,k -> new LinkedHashMap<>());
        for(Player p:room.players) {
            NetaState.Petals petals=state(room).petals.get(p.id);
            if(p.bankrupt || petals==null || petals.charges<=0 || decisions.containsKey(p.id)) continue;
            boolean atRisk=room.board.stream().anyMatch(t -> p.id.equals(t.ownerId) && t.level>0 && !PropertyEffects.frozen(t) && (all || Objects.equals(t.id,effect.tileId)));
            if(!atRisk) continue;
            ContentChoice c=content.open(room,p,"neta_petal_defense"); c.cardId=effect.id; c.content=effect.content; c.numbers=List.of(petals.charges);
            c.prompt="可授权最多"+petals.charges+"片花瓣抵挡本次常规建筑破坏；先消耗地产护盾，只有实际挡住破坏才扣花瓣。未用额度保留。"; return true;
        }
        return false;
    }
    /** 输入实际将受损的地产，护盾之后仅消费本次效果已授权的共享花瓣。 */
    public static boolean prevent(Room room,Tile tile) {
        if(room.runtime.effects.isEmpty()) return false;
        Map<String,Integer> budgets=state(room).defenses.get(room.runtime.effects.getFirst().id);
        NetaState.Petals petals=state(room).petals.get(tile.ownerId);
        if(budgets==null || budgets.getOrDefault(tile.ownerId,0)<=0 || petals==null || petals.charges<=0) return false;
        budgets.put(tile.ownerId,budgets.get(tile.ownerId)-1); petals.charges--; log(room,tile.name+" 消耗1片千本樱挡住建筑破坏，剩余"+petals.charges+"片"); return true;
    }
    /** 输入公开地产，返回神君和花瓣提示，不包含研究秘密选择。 */
    public static List<String> labels(Room room,Tile tile) {
        List<String> result=new ArrayList<>();
        if(room.runtime==null) return result;
        state(room).thunder.forEach((id,mark) -> { if(mark.tileId==tile.id) result.add("神君蓄势 "+mark.hits+"击 · "+player(room,id).name); });
        if(tile.petalCasterId!=null) result.add("千本樱：下一笔租金按低一级计算"); return result;
    }
}
