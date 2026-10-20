package com.dfw.domain;

import java.util.*;
import static com.dfw.domain.GameError.require;
import static com.dfw.domain.GameSupport.*;

/** 免费建造、资源转换、远程施工与多人奖励；复用原私人选择和结算队列。 */
public final class ConstructionService {
    /** 新增可主动使用的建设牌型。 */
    public static final Set<String> CARDS = Set.of("geoResonance","growthLink","sereniteaPot","createBuild","remoteConstruction","shrineGrowth");
    private ConstructionService() {}

    /** 输入房间，返回旧同格式存档可缺省的建设状态。 */
    public static ConstructionState state(Room room) {
        if(room.runtime.construction==null) room.runtime.construction=new ConstructionState();
        return room.runtime.construction;
    }

    /** 输入房间、玩家及地产，检查产权、满级、抵押、冻结和当回合建造次数。 */
    public static boolean eligible(Room room,Player owner,Tile target) {
        return movable(room,owner,target) && target.level<Rules.MAX_LEVEL;
    }

    /** 输入房间、玩家及地产，返回是否可参与等级重新分配，允许已经满级。 */
    private static boolean movable(Room room,Player owner,Tile target) {
        return owner!=null && !owner.bankrupt && target!=null && target.type.equals("property") && Objects.equals(target.ownerId,owner.id)
                && !target.mortgaged && !PropertyEffects.frozen(target) && !AnimeService.iced(target) && !room.upgradedTiles.contains(target.id);
    }

    /** 输入房间、业主及合法地产，免费增一级并登记实际新建数量，无递归联动。 */
    private static void build(Room room,Player owner,Tile target) {
        int before=target.level; target.level++;
        if(!room.upgradedTiles.contains(target.id)) room.upgradedTiles.add(target.id);
        SettlementService.observe(room); owner.matchStats.builtLevels++;
        log(room,"log",owner.id,owner.name+" 建筑升级 "+target.name+"（"+target.id+"号），建筑 "+before+" → "+target.level+" 级");
        BoardEffect.cue(room,"upgrade",owner);
    }

    /** 输入内容服务和主动操作；目标和资源仅确认时校验、消费，取消保留卡。 */
    public static void use(ContentService content,Room room,Player owner,Card card) {
        List<Integer> targets=room.board.stream().filter(t -> card.kind.equals("sereniteaPot") ? movable(room,owner,t) : eligible(room,owner,t)).map(t -> t.id).toList();
        if(!card.kind.equals("remoteConstruction")) require(!targets.isEmpty(),"当前没有可建设的合法自家地产，此卡保留");
        if(Set.of("growthLink","sereniteaPot").contains(card.kind)) require(targets.size()>=2,"此卡需要两处合法自家地产");
        if(card.kind.equals("sereniteaPot")) require(targets.stream().anyMatch(id -> tile(room,id).level<Rules.MAX_LEVEL),"两处地产均满级时不能新增建筑");
        if(card.kind.equals("createBuild")) require(owner.hand.size()>1,"需要至少一张其他手牌作为创造材料");
        ContentChoice choice=content.open(room,owner,"construction_"+card.kind); choice.cardId=card.id; choice.content=card; choice.tileIds.addAll(targets);
        choice.prompt=card.effect;
        if(card.kind.equals("createBuild")) choice.cards.addAll(owner.hand.stream().filter(c -> !c.id.equals(card.id)).toList());
    }

    /** 输入事件、触发者与随机源；锁定奖励接收者，再逐人开放选择。 */
    public static void event(ContentService content,Room room,Player owner,ContentResult source,RandomSource random) {
        ConstructionState s=state(room); require(s.tasks.isEmpty(),"上一批建设奖励尚未完成"); s.eventBuiltTiles.clear();
        if(source.mechanism.equals("allConstruction")) {
            for(Player p:room.players) if(!p.bankrupt) task(s,p,source,false);
        } else {
            List<Player> alive=room.players.stream().filter(p -> !p.bankrupt).toList();
            Map<String,Integer> levels=new HashMap<>(); alive.forEach(p -> levels.put(p.id,0));
            for(Tile t:room.board) if(levels.containsKey(t.ownerId)) levels.merge(t.ownerId,t.level,Integer::sum);
            int lowest=Collections.min(levels.values()); List<Player> candidates=alive.stream().filter(p -> levels.get(p.id)==lowest).toList();
            Player target=candidates.get(random.nextInt(candidates.size())); source.targetId=target.id;
            task(s,owner,source,false); task(s,target,source,owner.id.equals(target.id));
            log(room,"黄金树恩惠锁定建筑总等级最低者："+target.name+"（"+lowest+"级，并列时随机）");
        }
        resume(content,room);
    }

    /** 输入内部队列、已锁定玩家和来源，追加一份有限建造任务。 */
    private static void task(ConstructionState state,Player owner,ContentResult source,boolean repeat) {
        ConstructionState.BuildTask task=new ConstructionState.BuildTask(); task.playerId=owner.id; task.source=source; task.allowRepeat=repeat; state.tasks.add(task);
    }

    /** 输入房间与任务，返回仍有资格的地产；黄金树同人第二次可重复同事件建造。 */
    private static List<Integer> taskTargets(Room room,ConstructionState.BuildTask task) {
        Player owner=player(room,task.playerId); ConstructionState s=state(room);
        return room.board.stream().filter(t -> eligible(room,owner,t) || task.allowRepeat && owner!=null && !owner.bankrupt
                && s.eventBuiltTiles.contains(t.id) && Objects.equals(t.ownerId,owner.id) && !t.mortgaged && !PropertyEffects.frozen(t) && !AnimeService.iced(t) && t.level<Rules.MAX_LEVEL).map(t -> t.id).toList();
    }

    /** 输入父选择已恢复的房间，跳过无目标任务，下一位离线仍由原等待机制处理。 */
    private static void resume(ContentService content,Room room) {
        ConstructionState s=state(room);
        while(!s.tasks.isEmpty()) {
            ConstructionState.BuildTask task=s.tasks.getFirst(); List<Integer> targets=taskTargets(room,task);
            if(targets.isEmpty()) { log(room,"本次建设奖励无合法目标，跳过且不补现金"); s.tasks.removeFirst(); continue; }
            ContentChoice choice=content.open(room,player(room,task.playerId),"construction_event"); choice.content=task.source; choice.tileIds.addAll(targets);
            choice.prompt="选择一处地产免费升一级，也可以放弃。本批剩余奖励 "+s.tasks.size()+" 份。"; return;
        }
        s.eventBuiltTiles.clear();
    }

    /** 输入建设选择，复核所有参数后原子提交；失败保留选择与全部资源。 */
    public static void decide(ContentService content,Room room,Player owner,ContentChoice choice,Action action) {
        require(action.accept!=null,"请选择确认或取消");
        if(Boolean.TRUE.equals(action.accept)) {
            if(choice.type.equals("construction_event")) {
                ConstructionState.BuildTask task=state(room).tasks.getFirst(); require(Objects.equals(task.playerId,owner.id) && taskTargets(room,task).contains(action.tileId),"建设目标已失效");
                build(room,owner,tile(room,action.tileId)); state(room).eventBuiltTiles.add(action.tileId);
            } else if(choice.type.equals("construction_remote")) remote(room,owner,action.tileId);
            else execute(room,owner,ContentService.find(owner,choice.cardId),choice,action);
        }
        room.pending=choice.returnPending; room.phase=choice.returnPhase; room.runtime.choice=null;
        if(choice.type.equals("construction_event")) { state(room).tasks.removeFirst(); resume(content,room); }
    }

    /** 输入已经确认的手牌选择，先校验整批目标再消费，不允许重复地产或替换材料。 */
    private static void execute(Room room,Player owner,Card card,ContentChoice choice,Action action) {
        List<Integer> ids=action.tileIds==null ? action.tileId==null ? List.of() : List.of(action.tileId) : action.tileIds;
        require((card.kind.equals("remoteConstruction") || !ids.isEmpty()) && ids.size()<=2 && new HashSet<>(ids).size()==ids.size() && choice.tileIds.containsAll(ids),"请选择本次候选中的不同地产");
        List<Tile> targets=ids.stream().map(id -> tile(room,id)).toList();
        require(targets.stream().allMatch(t -> card.kind.equals("sereniteaPot") ? movable(room,owner,t) : eligible(room,owner,t)),"地产状态已变化，请重新选择");
        List<Card> materials=List.of(); int firstLevel=0,secondLevel=0;
        if(Set.of("growthLink","sereniteaPot").contains(card.kind)) require(ids.size()==2,"请选择两处不同地产");
        else if(!Set.of("createBuild","remoteConstruction").contains(card.kind)) require(ids.size()==1,"请选择一处中心地产");
        if(card.kind.equals("createBuild")) {
            require(action.discardCardIds!=null && action.discardCardIds.size()==ids.size() && new HashSet<>(action.discardCardIds).size()==ids.size() && !action.discardCardIds.contains(card.id),"每处建设需要一张不同的其他手牌");
            require(choice.cards.stream().filter(c -> action.discardCardIds.contains(c.id)).count()==ids.size(),"请选择原候选中的材料牌");
            materials=action.discardCardIds.stream().map(id -> ContentService.find(owner,id)).toList();
        }
        if(card.kind.equals("sereniteaPot")) {
            require(action.selection!=null && action.selection>=0 && action.selection<=Rules.MAX_LEVEL && ids.contains(action.tileId),"请设置两处地产的最终等级和新增建筑位置");
            firstLevel=action.selection; secondLevel=targets.getFirst().level+targets.getLast().level+1-firstLevel;
            require(secondLevel>=0 && secondLevel<=Rules.MAX_LEVEL && (action.tileId.equals(ids.getFirst()) ? firstLevel>0 : secondLevel>0),"最终等级总数必须等于原等级加一，且每处最高三级");
        }
        ContentService.consume(room,owner,card,true,action);
        switch(card.kind) {
            case "geoResonance" -> {
                Tile center=targets.getFirst(); List<Tile> line=List.of(tile(room,Math.floorMod(center.id-1,room.board.size())),center,tile(room,(center.id+1)%room.board.size()));
                for(Tile target:line) if(eligible(room,owner,target)) build(room,owner,target);
            }
            case "growthLink" -> {
                ConstructionState s=state(room); s.links.removeIf(link -> link.ownerId.equals(owner.id) && Set.of(link.first,link.second).equals(new HashSet<>(ids)));
                ConstructionState.Link link=new ConstructionState.Link(); link.ownerId=owner.id; link.first=ids.getFirst(); link.second=ids.getLast(); link.untilTurn=ended(room,owner.id)+4; s.links.add(link);
                log(room,owner.name+" 建立所闻遍计连接，三个后续个人回合内最多成功联动两次");
            }
            case "sereniteaPot" -> {
                int[] levels={firstLevel,secondLevel};
                for(int i=0;i<targets.size();i++) { Tile target=targets.get(i); if(target.level!=levels[i] || target.id==action.tileId) room.upgradedTiles.add(target.id); if(target.level>levels[i]) { target.damageOwnerId=null; target.damageLevel=0; target.damageUntilTurn=0; } target.level=levels[i]; target.damageOwnerId=null; target.damageLevel=0; target.damageUntilTurn=0; }
                SettlementService.observe(room); owner.matchStats.builtLevels++; log(room,owner.name+" 调整尘歌壶布局，建筑总数新增一级，最终等级 "+firstLevel+"／"+secondLevel); BoardEffect.cue(room,"upgrade",owner);
            }
            case "createBuild" -> { owner.hand.removeAll(materials); targets.forEach(t -> build(room,owner,t)); }
            case "remoteConstruction" -> { state(room).remoteUntil.put(owner.id,ended(room,owner.id)+2); log(room,owner.name+" 开启基建轮班，本回合及下一个人回合各可远程七五折建造一次"); }
            case "shrineGrowth" -> { ConstructionState.Shrine shrine=new ConstructionState.Shrine(); shrine.ownerId=owner.id; state(room).shrines.put(ids.getFirst(),shrine); log(room,targets.getFirst().name+" 开始接受奉纳，其他玩家正常停留结算后最多生长两次"); }
            default -> throw new GameError("建设牌型不可用");
        }
    }

    /** 输入本人回合房间与身份，返回尚未过期、未用且有足额现金的远程施工资格。 */
    public static boolean remoteAvailable(Room room,Player owner) {
        ConstructionState s=room.runtime==null ? null : room.runtime.construction;
        return s!=null && owner!=null && !owner.bankrupt && current(room)==owner && room.phase.equals("manage") && room.pending==null && room.trade==null
                && s.remoteUntil.getOrDefault(owner.id,0)>s.endedTurns.getOrDefault(owner.id,0) && s.remoteUsedTurn.getOrDefault(owner.id,-1)!=room.turnNumber
                && room.board.stream().anyMatch(t -> eligible(room,owner,t) && owner.cash>=remoteCost(room,t));
    }

    /** 输入本局与目标地产，返回标准建造费七五折后向下取整的实际应付金额。 */
    public static long remoteCost(Room room,Tile target) { return Math.multiplyExact(Rules.buildCost(room,target),75)/100; }

    /** 输入内容服务及本人身份，复用地产选择开启远程施工，不提前扣现金或次数。 */
    public static void openRemote(ContentService content,Room room,Player owner) {
        require(remoteAvailable(room,owner),"当前没有可用的远程施工机会或现金不足");
        ContentChoice choice=content.open(room,owner,"construction_remote");
        choice.tileIds.addAll(room.board.stream().filter(t -> eligible(room,owner,t) && owner.cash>=remoteCost(room,t)).map(t -> t.id).toList());
        choice.prompt="本次远程建造按标准费用75%支付，向下取整；无需停留，每回合一次，取消不消耗机会。";
    }

    /** 输入确认地块，支付折后现金并计入正常付费建造，最多触发一次连接链。 */
    private static void remote(Room room,Player owner,Integer id) {
        // 决策窗口已替换manage阶段，只复核保存的次数和地产，避免依赖窗口外的可见性判断。
        ConstructionState s=state(room); Tile target=tile(room,id);
        require(s.remoteUntil.getOrDefault(owner.id,0)>ended(room,owner.id) && s.remoteUsedTurn.getOrDefault(owner.id,-1)!=room.turnNumber && eligible(room,owner,target),"远程施工资格或目标已变化");
        long cost=remoteCost(room,target); require(owner.cash>=cost,"远程建造仅使用已有现金");
        owner.cash-=cost; build(room,owner,target); s.remoteUsedTurn.put(owner.id,room.turnNumber);
        log(room,owner.name+" 远程建造支付 ¥"+cost+"，现金余额 ¥"+owner.cash); paid(room,owner,target);
    }

    /** 输入一次已经完成的付费建造，触发相连地产免费升级；免费升级不会递归调用本方法。 */
    public static void paid(Room room,Player owner,Tile target) {
        if(room.runtime.construction==null) return;
        prune(room);
        for(ConstructionState.Link link:new ArrayList<>(state(room).links)) if(link.ownerId.equals(owner.id) && (link.first==target.id || link.second==target.id)) {
            Tile other=tile(room,link.first==target.id ? link.second : link.first);
            if(link.charges>0 && eligible(room,owner,other)) { build(room,owner,other); link.charges--; log(room,"所闻遍计成功联动，剩余 "+link.charges+" 次"); }
        }
        prune(room);
    }

    /** 输入已经正常落点的玩家和地产，等待费用或购买结算后再判断奉纳，不改变本次租金。 */
    public static void landing(Room room,Player visitor,Tile target,EffectResolver effects) {
        if(room.runtime.construction==null || !state(room).shrines.containsKey(target.id)) return;
        GameEffect effect=effects.effect("CONSTRUCTION_SHRINE",visitor); effect.tileId=target.id; effect.targetId=target.ownerId; effects.submit(room,effect);
    }

    /** 输入原队列恢复后的奉纳效果，核对原业主，合法才消费生长次数。 */
    public static void shrine(Room room,GameEffect effect) {
        prune(room); ConstructionState.Shrine mark=state(room).shrines.get(effect.tileId); Tile target=tile(room,effect.tileId);
        if(mark==null || Objects.equals(mark.ownerId,effect.playerId) || !Objects.equals(mark.ownerId,effect.targetId)) return;
        Player owner=player(room,mark.ownerId); if(eligible(room,owner,target)) { build(room,owner,target); mark.charges--; }
        prune(room);
    }

    /** 输入行动结束者，递增个人回合并清理到期连接或施工机会；休息回合同样计数。 */
    public static void endTurn(Room room,Player owner) {
        if(owner==null || room.runtime.construction==null) return;
        state(room).endedTurns.merge(owner.id,1,Integer::sum); prune(room);
    }

    /** 输入玩家标识，读取已结束个人回合数，旧存档默认零。 */
    private static int ended(Room room,String id) { return state(room).endedTurns.getOrDefault(id,0); }

    /** 输入房间，移除到期、淘汰、产权变化或耗尽的标记，不刷新剩余次数。 */
    private static void prune(Room room) {
        ConstructionState s=state(room);
        s.links.removeIf(link -> { Player owner=player(room,link.ownerId); return owner==null || owner.bankrupt || link.charges<=0 || ended(room,link.ownerId)>=link.untilTurn
                || !Objects.equals(tile(room,link.first).ownerId,link.ownerId) || !Objects.equals(tile(room,link.second).ownerId,link.ownerId); });
        s.shrines.entrySet().removeIf(entry -> { Player owner=player(room,entry.getValue().ownerId); return owner==null || owner.bankrupt || entry.getValue().charges<=0 || !Objects.equals(tile(room,entry.getKey()).ownerId,owner.id); });
        s.remoteUntil.entrySet().removeIf(entry -> { Player owner=player(room,entry.getKey()); return owner==null || owner.bankrupt || ended(room,entry.getKey())>=entry.getValue(); });
    }

    /** 输入房间和被清除的地产，同时移除其奉纳与涉及该地的完整连接。 */
    public static void clear(Room room,Tile target) {
        if(room.runtime.construction==null) return;
        state(room).shrines.remove(target.id); state(room).links.removeIf(link -> link.first==target.id || link.second==target.id);
    }

    /** 输入房间及地产，生成只含公开标记的说明，不泄漏手牌或私人选择。 */
    public static List<String> labels(Room room,int tileId) {
        if(room.runtime==null || room.runtime.construction==null) return List.of();
        ConstructionState s=room.runtime.construction; List<String> labels=new ArrayList<>();
        for(ConstructionState.Link link:s.links) if(link.charges>0 && (link.first==tileId || link.second==tileId) && s.endedTurns.getOrDefault(link.ownerId,0)<link.untilTurn
                && Objects.equals(tile(room,link.first).ownerId,link.ownerId) && Objects.equals(tile(room,link.second).ownerId,link.ownerId)) labels.add("所闻遍计：连接"+(link.first==tileId ? link.second : link.first)+"号地，剩余"+link.charges+"次");
        ConstructionState.Shrine shrine=s.shrines.get(tileId); if(shrine!=null && shrine.charges>0 && Objects.equals(tile(room,tileId).ownerId,shrine.ownerId)) labels.add("奉纳：剩余"+shrine.charges+"次生长");
        return labels;
    }
}
