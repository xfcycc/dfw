package com.dfw.domain;

import com.dfw.domain.rules.ContentCatalog;
import java.util.*;
import static com.dfw.domain.GameError.require;
import static com.dfw.domain.GameSupport.*;

/** 扩展道具、地产标记和多人事件，复用私人选择及房间原子快照。 */
public final class ExpansionService {
    private final ContentService content;
    private final EffectResolver effects;
    private final RandomSource random;
    private final ContentCatalog catalog;
    /** 输入既有内容服务、效果队列及目录，注册闷棍解控后的偷牌阶段。 */
    public ExpansionService(ContentService content, EffectResolver effects, RandomSource random, ContentCatalog catalog) {
        this.content=content; this.effects=effects; this.random=random; this.catalog=catalog;
        effects.register("SAP_STEAL", (room,effect) -> steal(room,player(room,effect.playerId),player(room,effect.targetId)));
    }
    /** 输入房间，返回当前可施加附加效果的普通地产。 */
    private static List<Tile> properties(Room room) {
        return room.board.stream().filter(t -> t.type.equals("property") && !PropertyEffects.frozen(t)).toList();
    }
    /** 输入候选列表，返回均匀随机顺序，不使用地图种子。 */
    private <T> List<T> shuffled(List<T> values) {
        List<T> result=new ArrayList<>(values);
        for(int i=result.size()-1;i>0;i--) { int j=random.nextInt(i+1); Collections.swap(result,i,j); }
        return result;
    }
    /** 输入已抽取事件，立即记录标记或随机结果；全员补给统一完成后整理超限。 */
    public void event(Room room,Player actor,ContentResult result) {
        switch(result.mechanism) {
            case "treasure" -> {
                List<Tile> candidates=properties(room).stream().filter(t -> !t.treasure).toList();
                if(candidates.isEmpty()) { log(room,"当前没有可标记宝藏的地产，本次寻宝结束"); return; }
                Tile target=candidates.get(random.nextInt(candidates.size())); target.treasure=true;
                log(room,"大海贼的宝藏出现在 "+target.name+"（"+target.id+"号），首位正常停留者可领取，按到达时无修正租金计算");
            }
            case "concert" -> {
                for(Tile target:shuffled(properties(room)).stream().limit(5).toList()) {
                    target.concertStacks=Math.addExact(target.concertStacks,1);
                    log(room,target.name+" 获得演唱会效果，当前 "+target.concertStacks+" 层，下一次正租金触发时全部消耗");
                }
            }
            case "shuffle" -> {
                List<Player> alive=room.players.stream().filter(p -> !p.bankrupt).toList();
                if(alive.size()<2) return;
                List<Integer> indices=new ArrayList<>(); for(int i=0;i<alive.size();i++) indices.add(i);
                List<Integer> order;
                do { order=shuffled(indices); } while(hasFixedPoint(order));
                List<Integer> positions=alive.stream().map(p -> p.position).toList();
                for(int i=0;i<alive.size();i++) relocate(room,alive.get(i),positions.get(order.get(i)));
                log(room,"ROOM·错位空间：所有未破产玩家均接手另一人的原位置，不结算落点或起点");
            }
            case "all_cards" -> {
                for(Player target:room.players) if(!target.bankrupt) content.giveCards(room,target,1,null);
                log(room,"四次元口袋：所有未破产玩家各收到一份私人道具补给"); content.overflow(room);
            }
            default -> throw new GameError("内容机制不可用");
        }
    }
    /** 输入位置索引排列，返回是否有玩家仍拿到自己的原位置。 */
    private static boolean hasFixedPoint(List<Integer> order) {
        for(int i=0;i<order.size();i++) if(order.get(i)==i) return true;
        return false;
    }
    /** 输入房间、玩家和目标格，只保存直接位移片段，不结算路径与落点。 */
    private void relocate(Room room,Player target,int position) {
        Motion motion=new Motion(); motion.id=random.id(); motion.playerId=target.id; motion.from=target.position; motion.teleport=true;
        target.position=position; motion.path.add(position);
        MotionSegment segment=new MotionSegment(); segment.id=motion.id; segment.playerId=target.id; segment.from=motion.from; segment.teleport=true; segment.path=motion.path;
        room.movementSequence.add(segment); motion.segments=new ArrayList<>(room.movementSequence); room.motion=motion;
        log(room,target.name+" 直接移动至 "+room.board.get(position).name+"（"+position+"号）");
    }
    /** 输入作用双方，将目标的一张随机手牌私下转交；空手时仍保留此前闷棍效果。 */
    private void steal(Room room,Player actor,Player target) {
        if(actor==null || target==null || actor.bankrupt || target.bankrupt || target.hand.isEmpty()) return;
        Card card=target.hand.remove(random.nextInt(target.hand.size())); actor.hand.add(card);
        actor.contentNotice=card; actor.contentNoticeText="闷棍偷取了这张道具";
        target.contentNotice=card; target.contentNoticeText="这张道具被转移给 "+actor.name;
        log(room,actor.name+" 从 "+target.name+" 处偷走一张私人手牌"); content.overflow(room);
    }
    /** 输入当前主动卡及目标，完成校验后才消费并执行，私人选择暂不消费。 */
    public void use(Room room,Player actor,Card card,Action action) {
        Tile target=tile(room,action.tileId); Player opponent=player(room,action.playerId);
        switch(card.kind) {
            case "recycle","discover" -> {
                require(!card.kind.equals("recycle") || actor.hand.size()>1,"没有其他旧牌可供制衡");
                ContentChoice choice=content.open(room,actor,card.kind); choice.cardId=card.id; choice.content=card;
                if(card.kind.equals("recycle")) choice.cards.addAll(actor.hand.stream().filter(c -> !c.id.equals(card.id)).toList());
                else {
                    if(card.discoverCandidates.isEmpty()) card.discoverCandidates.addAll(catalog.discover(room,random).stream().map(ContentService::card).toList());
                    choice.cards.addAll(card.discoverCandidates);
                }
            }
            case "shield","exchange","kamui","blackhole","homeTeleport" -> {
                require(target!=null && target.type.equals("property") && !PropertyEffects.frozen(target),"请选择未处于神威中的地产");
                if(card.kind.equals("shield")) require(Objects.equals(target.ownerId,actor.id) && !target.demolitionShield,"请选择尚无护盾的自家地产");
                if(card.kind.equals("homeTeleport")) require(Objects.equals(target.ownerId,actor.id),"请选择自家地产");
                if(card.kind.equals("blackhole")) require(target.barrier || target.meteorCharges>0 || target.demolitionShield || target.treasure || target.concertStacks>0,"该地产没有可清除的附加效果");
                Player landlord=player(room,target.ownerId); long price=PropertyEffects.baseRent(room,target);
                if(card.kind.equals("exchange")) require(landlord!=null && !landlord.bankrupt && landlord!=actor && actor.cash>=price,"请选择付得起无修正租金的对手地产");
                ContentService.consume(room,actor,card,true,action);
                switch(card.kind) {
                    case "shield" -> target.demolitionShield=true;
                    case "exchange" -> {
                        actor.cash-=price; landlord.cash=Math.addExact(landlord.cash,price); target.ownerId=actor.id;
                        log(room,actor.name+" 支付 "+landlord.name+" ¥"+price+"，取得 "+target.name+"（"+target.id+"号），承接建筑、抵押和附加效果");
                    }
                    case "kamui" -> { target.kamuiCasterId=actor.id; target.kamuiTurns=3; target.kamuiAppliedTurn=room.turnNumber; }
                    case "blackhole" -> { target.barrier=false; target.meteorCharges=0; target.demolitionShield=false; target.treasure=false; target.concertStacks=0; }
                    case "homeTeleport" -> teleport(room,actor,target.id);
                    default -> throw new IllegalStateException("未处理的地产道具");
                }
            }
            case "sap","slow","reaper","shadowstep" -> {
                require(opponent!=null && !opponent.bankrupt && opponent!=actor,"请选择其他未破产玩家");
                if(card.kind.equals("sap")) require(opponent.position==actor.position,"闷棍只能作用于同格玩家");
                if(card.kind.equals("reaper")) require(PropertyEffects.handLimit(actor)>1 && PropertyEffects.handLimit(opponent)>1,"双方手牌上限须大于一张");
                ContentService.consume(room,actor,card,true,action);
                switch(card.kind) {
                    case "sap" -> {
                        content.restrict(room,opponent,1,false);
                        GameEffect effect=effects.effect("SAP_STEAL",actor); effect.targetId=opponent.id; effects.submit(room,effect);
                    }
                    case "slow" -> { GameEffect effect=effects.effect("ADD_STATUS",opponent); effect.statusId="NEXT_ROLL_HALF"; effect.amount=1; effects.submit(room,effect); }
                    case "reaper" -> {
                        actor.handLimitReduction++; opponent.handLimitReduction++;
                        log(room,actor.name+" 与 "+opponent.name+" 的本局手牌上限分别降为 "+PropertyEffects.handLimit(actor)+"、"+PropertyEffects.handLimit(opponent)+" 张"); content.overflow(room);
                    }
                    case "shadowstep" -> teleport(room,actor,opponent.position);
                    default -> throw new IllegalStateException("未处理的玩家道具");
                }
            }
            case "swapAny" -> {
                Player second=player(room,action.secondPlayerId);
                require(opponent!=null && second!=null && opponent!=second && !opponent.bankrupt && !second.bankrupt,"请选择两名不同的未破产玩家");
                ContentService.consume(room,actor,card,true,action); int first=opponent.position, other=second.position;
                relocate(room,opponent,other); relocate(room,second,first);
                log(room,opponent.name+" 与 "+second.name+" 斗转星移，不结算落点或起点");
            }
            case "handDestruction" -> {
                ContentService.consume(room,actor,card,true,action);
                for(Player owner:room.players) if(!owner.bankrupt) { int count=owner.hand.size(); owner.hand.clear(); content.giveCards(room,owner,count,null); }
                log(room,"手牌抹杀：所有未破产玩家的剩余手牌已等量重抽，弃牌不折现"); content.overflow(room);
            }
            case "gravity" -> {
                ContentService.consume(room,actor,card,true,action);
                for(Player owner:room.players) if(owner!=actor && !owner.bankrupt) relocate(room,owner,actor.position);
                log(room,"地爆天星：其他玩家聚集至 "+actor.name+" 所在格，不结算任何落点");
            }
            case "meteor" -> {
                List<Tile> candidates=properties(room); require(!candidates.isEmpty(),"没有可放置陨星的地产");
                ContentService.consume(room,actor,card,true,action); int count=Math.min(1+random.nextInt(3),candidates.size());
                for(Tile property:shuffled(candidates).stream().limit(count).toList()) {
                    property.meteorCharges=Math.addExact(property.meteorCharges,1);
                    log(room,"陨星落在 "+property.name+"（"+property.id+"号），剩余陨星截停次数 "+property.meteorCharges);
                }
            }
            default -> throw new GameError("该道具不可主动使用");
        }
    }
    /** 输入定向传送终点，入队后由回合服务替代骰子并正常结算落点。 */
    private void teleport(Room room,Player actor,int tileId) {
        GameEffect effect=effects.effect("DIRECTED_TELEPORT",actor); effect.tileId=tileId; effects.submit(room,effect);
    }
    /** 输入已保存候选及确认，校验选择后消费发动卡并交换或授予私人手牌。 */
    public void decide(Room room,Player actor,ContentChoice choice,Action action) {
        require(action.accept!=null,"请选择确认或取消"); if(!action.accept) return;
        Card source=ContentService.find(actor,choice.cardId);
        if(choice.type.equals("recycle")) {
            require(action.discardCardIds!=null && !action.discardCardIds.isEmpty(),"请至少选择一张旧牌");
            Set<String> ids=new HashSet<>(action.discardCardIds);
            require(ids.size()==action.discardCardIds.size() && !ids.contains(source.id) && choice.cards.stream().filter(c -> ids.contains(c.id)).count()==ids.size() && actor.hand.stream().filter(c -> ids.contains(c.id)).count()==ids.size(),"制衡候选已变化");
            ContentService.consume(room,actor,source,true,action); actor.hand.removeIf(c -> ids.contains(c.id)); content.giveCards(room,actor,ids.size(),null);
        } else {
            Card selected=choice.cards.stream().filter(c -> Objects.equals(c.id,action.cardId)).findFirst().orElseThrow(() -> new GameError("请选择本次发现候选"));
            ContentService.consume(room,actor,source,true,action); actor.hand.add(selected); actor.contentNotice=selected; actor.contentNoticeText="发现获得这张道具";
        }
        log(room,actor.name+" 已完成私人选牌，弃置牌不折现");
    }
}
