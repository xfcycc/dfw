package com.dfw.domain;

import java.util.*;
import static com.dfw.domain.GameSupport.*;

/** 回合、移动、落点和在线暂停规则。 */
public class TurnService {
    private final EconomyService economy;
    private final RandomSource random;
    private final EffectResolver effects;
    private final ContentService content;
    private final ActivityService activities;
    /** 输入经济规则、随机源与结算服务，注册回合、移动、传送及指定入狱效果。 */
    public TurnService(EconomyService economy, RandomSource random, EffectResolver effects, ContentService content, ActivityService activities) {
        this.economy = economy; this.random = random; this.effects = effects; this.content = content; this.activities = activities;
        effects.register("DRAW_CARDS", this::drawCards);
        effects.register("DIRECTED_TELEPORT", this::directedTeleport);
        effects.register("SKIP_TURN", this::skipTurn);
        effects.register("MOVE", this::moveEffect);
        effects.register("PORTAL_TRANSFER", this::portalTransfer);
        effects.register("RANDOM_TELEPORT", this::teleport);
        effects.register("SEND_TO_JAIL", this::sendToJail);
        effects.register("SETTLE_TILE", (room, effect) -> settle(room, player(room, effect.playerId)));
        effects.register("PAY_FEE", (room, effect) -> {
            economy.charge(room, player(room, effect.playerId), effect.amount, effect.targetId, effect.tileId);
            if (paymentPending(room) && room.pending.playerId.equals(effect.playerId) && effect.content != null) room.pending.sourceName = effect.content.name;
        });
        effects.register("SETTLE_PAYMENT", (room, effect) -> economy.pay(room, player(room, effect.playerId)));
        effects.register("DECLARE_BANKRUPTCY", (room, effect) -> {
            Player payer = player(room, effect.playerId); economy.declareBankruptcy(room, payer);
            if (room.runtime.globalEvent == null && room.activity == null) { if (current(room) == payer) advance(room); else checkWinner(room); }
        });
    }
    /** 输入玩家和数量，增加私人道具，不记录种类与数量。 */
    public void giveCards(Room room, Player player, int count) { content.giveCards(room, player, count, null); }
    /** 输入私人补给效果，增加作用玩家手牌，公开日志不包含种类与数量。 */
    private void drawCards(Room room, GameEffect effect) {
        Player owner = player(room, effect.playerId); if (owner == null || owner.bankrupt) return;
        giveCards(room, owner, Math.toIntExact(effect.amount)); content.overflow(room); log(room, owner.name + " 收到一份私人补给");
    }
    /** 输入休息效果，设置后续跳过次数，保留普通休息与监禁的既有区别。 */
    private void skipTurn(Room room, GameEffect effect) {
        Player owner = player(room, effect.playerId); if (owner == null || owner.bankrupt) return;
        content.restrict(room, owner, Math.toIntExact(effect.amount), false);
    }
    /** 输入房间，仅余一人时结束游戏并返回真。 */
    public boolean checkWinner(Room room) {
        List<Player> alive = room.players.stream().filter(p -> !p.bankrupt).toList();
        if (alive.size() > 1) return false;
        SettlementService.winner(room, alive.isEmpty() ? null : alive.getFirst());
        room.activity = null; room.status = "finished"; room.winnerId = alive.isEmpty() ? null : alive.getFirst().id;
        room.pending = null; room.trade = null; room.pausedReason = null; if(room.runtime!=null) room.runtime.choice=null;
        log(room, alive.isEmpty() ? "本局结束" : alive.getFirst().name + " 成为最后的赢家！");
        HistoryTrace.event(room, "game_finished", room.winnerId, "本局结束", null); return true;
    }
    /** 输入房间，切换下一行动者并重置回合标志。 */
    public void advance(Room room) {
        HistoryTrace.event(room, "turn_end", current(room) == null ? null : current(room).id, "行动结束", null);
        ConstructionService.endTurn(room,current(room));
        CrossoverService.endTurn(room,current(room));
        AnimeService.endTurn(room,current(room));
        StatusService.endTurn(room, current(room));
        if (checkWinner(room)) return;
        activities.endTurn(room);
        do { room.turnIndex = (room.turnIndex + 1) % room.turnOrder.size(); } while (current(room).bankrupt);
        room.turnNumber++; room.phase = "pre_roll"; room.pending = null; room.dice = null;
        // 房主移出原行动者会终止其父结算；其他玩家的超限牌仍完整保留，由新回合重新开放整理。
        if(room.runtime!=null) room.runtime.choice=null;
        room.activeCardUsed = false; room.eventDrawn = false; room.upgradedTiles.clear();
        SettlementService.observe(room);
        HistoryTrace.event(room, "turn_start", current(room).id, "新行动开始", null);
    }
    /** 输入费用参数，打开可管理资产和协商抵债的支付选择。 */
    public void charge(Room room, Player player, long amount, String creditorId, Integer tileId) {
        GameEffect effect = effects.effect("PAY_FEE", player); effect.amount = amount; effect.targetId = creditorId; effect.tileId = tileId;
        effects.submit(room, effect);
    }
    /** 输入当前付款人，将确认支付插在父流程暂停的效果之前。 */
    public void pay(Room room, Player player) {
        GameEffect effect = effects.effect("SETTLE_PAYMENT", player); effect.resumesDecision = true; effects.submit(room, effect);
    }
    /** 输入当前债务人，将主动清算插入费用等待点，全图批次保持原游标。 */
    public void declareBankruptcy(Room room, Player player) {
        GameEffect effect = effects.effect("DECLARE_BANKRUPTCY", player); effect.resumesDecision = true; effects.submit(room, effect);
    }
    /** 输入房间和移动效果，执行实际路径并关联触发本段的公开事件，无返回值。 */
    private void moveEffect(Room room, GameEffect effect) {
        int first = room.movementSequence.size();
        move(room, player(room, effect.playerId), (int) effect.amount,effect.selector);
        if("NORMAL".equals(effect.selector) && "RANDOM".equals(room.runtime.rollMode) && CrossoverService.state(room).boostTurn==room.turnNumber) effects.submit(room,effects.effect("CROSS_BOOST",player(room,effect.playerId)));
        if (effect.content != null && !effect.content.privateSupply) room.movementSequence.stream().skip(first).filter(segment -> segment.boardEffect == null).findFirst().ifPresent(segment -> segment.beforeEvent = effect.content);
        if (effect.message != null) log(room, effect.message);
    }
    /** 输入房间、玩家及最终有符号步数，处理翻倍、路障与路径；正向经过或最终停在起点时发奖，无返回值。 */
    public void move(Room room, Player player, int steps) { move(room,player,steps,null); }
    /** 输入移动模式，突进忽略路径、喷射仅忽略陨星，后续仍只由原阶段结算最终落点。 */
    private void move(Room room,Player player,int steps,String mode) {
        move(room,player,steps,mode,false);
    }
    /** 输入已换算步数、移动模式及是否续走，续走不重复消费翻倍状态或领取原地奖励。 */
    private void move(Room room,Player player,int steps,String mode,boolean continuing) {
        boolean flash="FLASH".equals(mode), boost="BOOST".equals(mode);
        if (!continuing && !flash && !boost && StatusService.consume(player,"NEXT_MOVE_DOUBLE")) { steps=Math.multiplyExact(steps,2); log(room,player.name+" 消耗塔塔开，最终移动距离变为 "+steps+" 格"); BoardEffect.cue(room,"morale",player); }
        Motion motion = new Motion(); motion.id = random.id(); motion.playerId = player.id; motion.from = player.position;
        MotionSegment segment = new MotionSegment(); segment.id = motion.id; segment.playerId = player.id; segment.from = player.position; segment.path = motion.path;
        segment.dice = room.movementSequence.stream().noneMatch(value -> value.boardEffect == null) && room.phase.equals("pre_roll") ? room.dice : null;
        boolean rewarded = false, immune = false, portalPaused = false;
        BoardEffect.Observation trap = null;
        if (!continuing && steps == 0 && player.position == 0) { rewardStart(room, player, "在起点原地停留"); rewarded = true; }
        for (int i = 0; i < Math.abs(steps); i++) {
            player.position = Math.floorMod(player.position + Integer.signum(steps), room.board.size()); motion.path.add(player.position);
            if (player.position == 0 && steps > 0) { rewardStart(room, player, "向前经过起点"); rewarded = true; }
            Tile tile = room.board.get(player.position);
            if(flash) continue;
            if(!PropertyEffects.frozen(tile) && tile.portalTileId!=null) {
                Tile exit=tile(room,tile.portalTileId);
                if(exit!=null && !PropertyEffects.frozen(exit) && Objects.equals(exit.portalTileId,tile.id)) {
                    if(player.hand.stream().anyMatch(c -> c.kind.equals("release") && CrossoverService.handAllowed(room,player,c,false))) {
                        GameEffect transfer=effects.effect("PORTAL_TRANSFER",player); transfer.tileId=tile.id; transfer.destination=exit.id;
                        transfer.amount=(Math.abs(steps)-i-1)*Integer.signum(steps); transfer.selector=mode; transfer.statusId=steps<0?"BACKWARD":"FORWARD";
                        effects.submit(room,transfer); portalPaused=true; break;
                    }
                    tile.portalTileId=null; exit.portalTileId=null; player.position=exit.id; motion.path.add(exit.id); tile=exit;
                    log(room,player.name+" 通过虚空之泪到达 "+exit.name+"，继续剩余步数");
                }
            }
            if (PropertyEffects.frozen(tile)) continue;
            if ((tile.barrier || !boost && tile.meteorCharges>0) && StatusService.immune(player, "TRAP")) { log(room, player.name + " 免疫 " + tile.name + " 的路障，路障保留"); immune = true; continue; }
            if (tile.barrier || !boost && tile.meteorCharges>0) {
                trap = BoardEffect.watch(room,"barrierHit",player);
                if(tile.barrier) { tile.barrier=false; CrossoverService.state(room).boostBlocked=true; } else tile.meteorCharges--;
                log(room, player.name + " 在 " + tile.name + " 被路障或陨星拦下，每次仅消耗一层"); break;
            }
        }
        if (!portalPaused && steps < 0 && player.position == 0) { rewardStart(room, player, "后退停在起点"); rewarded = true; }
        room.movementSequence.add(segment); motion.segments = new ArrayList<>(room.movementSequence); room.motion = motion;
        if (rewarded) { BoardEffect.cue(room,"income",player); BoardEffect.cue(room,"supply",player); }
        if (immune) BoardEffect.cue(room,"trapImmune",player);
        if (trap != null) trap.finish(room);
        // 全部移动完成后才开放补给超限选择，保留后续落点阶段与已完成路径。
        if(!portalPaused) content.overflow(room);
    }
    /** 输入已固定入口、出口及剩余步数，响应后处理实际所在格路障再续走；抵消保留传送门。 */
    private void portalTransfer(Room room,GameEffect effect) {
        Player owner=player(room,effect.playerId); if(owner==null || owner.bankrupt) return;
        Tile entry=tile(room,effect.tileId), exit=tile(room,effect.destination);
        if(!effect.movementCancelled && entry!=null && exit!=null && !PropertyEffects.frozen(entry) && !PropertyEffects.frozen(exit) && Objects.equals(entry.portalTileId,exit.id) && Objects.equals(exit.portalTileId,entry.id)) {
            entry.portalTileId=null; exit.portalTileId=null;
            MotionSegment segment=BoardEffect.begin("teleport",owner); BoardEffect.movement(segment,owner,exit.id); owner.position=exit.id; BoardEffect.publish(room,segment);
            log(room,owner.name+" 通过虚空之泪到达 "+exit.name+"，继续剩余步数");
        }
        Tile landed=room.board.get(owner.position); boolean blocked=false;
        if(!PropertyEffects.frozen(landed) && (landed.barrier || !"BOOST".equals(effect.selector) && landed.meteorCharges>0)) {
            if(StatusService.immune(owner,"TRAP")) { log(room,owner.name+" 免疫 "+landed.name+" 的路障，路障保留"); BoardEffect.cue(room,"trapImmune",owner); }
            else {
                BoardEffect.Observation trap=BoardEffect.watch(room,"barrierHit",owner);
                if(landed.barrier) { landed.barrier=false; CrossoverService.state(room).boostBlocked=true; } else landed.meteorCharges--;
                log(room,owner.name+" 在 "+landed.name+" 被路障或陨星拦下，每次仅消耗一层"); trap.finish(room); blocked=true;
            }
        }
        if(!blocked && effect.amount!=0) move(room,owner,(int)effect.amount,effect.selector,true);
        else {
            if(owner.position==0 && "BACKWARD".equals(effect.statusId)) { rewardStart(room,owner,"后退停在起点"); BoardEffect.cue(room,"income",owner); BoardEffect.cue(room,"supply",owner); }
            content.overflow(room);
        }
    }
    /** 输入已确认传送终点，替代掷骰并保存路径；到起点仅领取一次奖励，落点交原 LANDING 阶段。 */
    private void directedTeleport(Room room, GameEffect effect) {
        Player actor=player(room,effect.playerId); Tile destination=tile(room,effect.tileId);
        GameError.require(destination!=null,"传送终点不可用");
        Motion motion=new Motion(); motion.id=random.id(); motion.playerId=actor.id; motion.from=actor.position; motion.teleport=true;
        actor.position=destination.id; motion.path.add(destination.id);
        MotionSegment segment=new MotionSegment(); segment.id=motion.id; segment.playerId=actor.id; segment.from=motion.from; segment.path=motion.path; segment.teleport=true;
        room.movementSequence.add(segment); motion.segments=new ArrayList<>(room.movementSequence); room.motion=motion;
        room.runtime.rollMode="DIRECTED"; room.runtime.windowExit="replace_roll";
        log(room,actor.name+" 传送至 "+destination.name+"（"+destination.id+"号），替代本次掷骰并正常结算");
        if(destination.id==0) { rewardStart(room,actor,"传送到达起点"); BoardEffect.cue(room,"income",actor); BoardEffect.cue(room,"supply",actor); }
        content.overflow(room);
    }
    /** 输入已校验的传送目标玩家，随机直达任意地块并保存动画；不走路径、不结算落点或消费振奋。 */
    private void teleport(Room room, GameEffect effect) {
        Player target=player(room,effect.targetId);
        GameError.require(target!=null && !target.bankrupt,"传送目标不可用");
        Motion motion=new Motion(); motion.id=random.id(); motion.playerId=target.id; motion.from=target.position; motion.teleport=true;
        target.position=random.nextInt(room.board.size()); motion.path.add(target.position);
        MotionSegment segment=new MotionSegment(); segment.id=motion.id; segment.playerId=target.id; segment.from=motion.from; segment.path=motion.path; segment.teleport=true;
        room.movementSequence.add(segment); motion.segments=new ArrayList<>(room.movementSequence); room.motion=motion;
        log(room,"log",target.id,target.name+" 被爆破鬼才传送至 "+room.board.get(target.position).name+"（"+target.position+"号），此次落点不结算");
    }
    /** 输入目标玩家及入狱效果，先核对外部控制保护再传送并开放解控；本人掷骰前入狱替代移动，无返回值。 */
    private void sendToJail(Room room, GameEffect effect) {
        Player target=player(room,effect.playerId);
        GameError.require(target!=null && !target.bankrupt,"请选择一名未破产玩家，可选择自己");
        if(!effect.voluntaryRestriction && StatusService.controlProtectionTurns(target)>0) { log(room,target.name+" 的连续控制保护抵挡了本次入狱及传送"); return; }
        Tile jail=room.board.stream().filter(t -> t.type.equals("jail")).findFirst().orElseThrow();
        Motion motion=new Motion(); motion.id=random.id(); motion.playerId=target.id; motion.from=target.position; motion.teleport=true;
        target.position=jail.id; motion.path.add(jail.id);
        MotionSegment segment=new MotionSegment(); segment.id=motion.id; segment.playerId=target.id; segment.from=motion.from; segment.path=motion.path; segment.teleport=true;
        room.movementSequence.add(segment); motion.segments=new ArrayList<>(room.movementSequence); room.motion=motion;
        if (target==current(room) && room.phase.equals("pre_roll")) { room.runtime.rollMode="JAIL"; room.runtime.windowExit="replace_roll"; }
        log(room,"log",target.id,target.name+" 被直接送至监狱，不触发沿途奖励或重复落点结算");
        content.restrict(room,target,1,true,!effect.voluntaryRestriction);
    }
    /** 输入房间、领奖玩家及触发动作，按本局配置发放起点现金与道具并记录日志，无返回值。 */
    private void rewardStart(Room room, Player player, String action) {
        long income = Rules.economy(room).money("startIncome", Rules.START_INCOME); int cards = Rules.startCards(room.players.size());
        player.cash += income; giveCards(room, player, cards);
        log(room, player.name + " " + action + "，获得 ¥" + income + " 和 " + cards + " 张道具补给");
    }
    /** 输入房间和落点玩家，结算当前地块；事件移动再次落入事件格时继续抽取，无返回值。 */
    public void settle(Room room, Player player) {
        Tile tile = room.board.get(player.position); room.phase = "manage"; room.pending = null;
        switch (tile.type) {
            case "activity" -> activities.open(room, player, tile);
            case "jail" -> { content.restrict(room, player, 1, true); }
            case "chance", "fortune" -> {
                if(room.eventDrawn) { log(room,"本个人回合已抽取事件，本次不再抽取"); break; }
                room.eventDrawn = true;
                content.event(room, player, tile.type);
            }
            case "property" -> {
                if(PropertyEffects.frozen(tile)) { log(room,player.name+" 停在神威中的 "+tile.name+"，不结算地产效果"); break; }
                boolean found=tile.treasure;
                if(found) { long reward=PropertyEffects.baseRent(room,tile); tile.treasure=false; player.cash=Math.addExact(player.cash,reward); log(room,player.name+" 在 "+tile.name+" 领取宝藏 ¥"+reward+"，本次不收租且保留全部租金状态"); }
                if (tile.ownerId == null) { room.phase = "buy"; room.pending = pending("buy", player, tile.id, tile.price, null); }
                else if (tile.ownerId.equals(player.id)) {
                    if (!tile.mortgaged && !AnimeService.iced(tile) && tile.level < Rules.MAX_LEVEL && !room.upgradedTiles.contains(tile.id)) { room.phase = "upgrade"; room.pending = pending("upgrade", player, tile.id, Rules.buildCost(room, tile), null); }
                } else if (!found && !tile.mortgaged) {
                    charge(room, player, Rules.rent(room, tile), tile.ownerId, tile.id);
                }
                ConstructionService.landing(room,player,tile,effects);
            }
            default -> { }
        }
    }
    /** 输入房间，返回房主是否离线满六十秒且有在线真人可接任；恢复分配期间不转交。 */
    public boolean hostTransferDue(Room room) {
        if (room.recoveryPending || room.hostDisconnectedAt == null || System.currentTimeMillis() - room.hostDisconnectedAt < 60_000) return false;
        Player host = player(room, room.hostId);
        return host != null && !host.online && room.players.stream().anyMatch(p -> p.online && p.botDifficulty == null);
    }
    /** 输入房间，为离线房主保留六十秒重连时间，维持机器人在线并更新断线暂停，无返回值。 */
    public void refresh(Room room) {
        room.players.stream().filter(p -> p.botDifficulty != null).forEach(p -> p.online = true);
        if (room.recoveryPending) { room.pausedReason = "等待房主分配角色并继续对局"; return; }
        Player host = player(room, room.hostId);
        if (host != null && host.botDifficulty == null && !host.online) {
            if (room.hostDisconnectedAt == null) room.hostDisconnectedAt = System.currentTimeMillis();
        } else room.hostDisconnectedAt = null;
        if (host == null || host.botDifficulty != null || hostTransferDue(room)) room.players.stream().filter(p -> p.online && p.botDifficulty == null).findFirst().ifPresent(p -> {
            room.hostId = p.id; room.hostDisconnectedAt = null;
        });
        if (!room.status.equals("playing")) { room.pausedReason = null; return; }
        Player actor = room.pending != null ? player(room, room.pending.playerId) : room.activity != null ? null : room.runtime != null && room.runtime.globalEvent != null ? null : current(room);
        Player offline = room.players.stream().filter(p -> !p.online && ((actor != null && p.id.equals(actor.id)) || (room.trade != null && p.id.equals(room.trade.toId)))).findFirst().orElse(null);
        Player required = activities.offlineRequired(room);
        if (required != null) offline = required;
        room.pausedReason = offline == null ? null : "等待 " + offline.name + " 重新连接";
        if (room.trade != null) {
            room.trade.remainingMs = 0; room.trade.runningSince = null;
        }
    }
    /** 输入配置对局，跳过在线玩家的一次休息行动并启动连控保护，服刑结束后恢复收租，实际跳过时返回真。 */
    public boolean skipCurrentTurn(Room room) {
        Player actor = current(room);
        if (actor == null || !actor.online || actor.skipTurns == 0 && CrossoverService.state(room).skippedTurn!=room.turnNumber || room.trade != null) return false;
        if(CrossoverService.state(room).skippedTurn==room.turnNumber) { if(!content.crossover.beforeEnd(room,actor)) advance(room); return true; }
        CrossoverService.state(room).skippedTurn=room.turnNumber;
        boolean jailed=actor.jailed; actor.skipTurns--; if(actor.voluntarySkipTurns>0) actor.voluntarySkipTurns--; if (actor.skipTurns==0) actor.jailed=false;
        StatusService.controlledSkip(room,actor);
        log(room, actor.name + (jailed ? actor.jailed ? " 服刑休息一回合，继续暂停收租" : " 服刑休息一回合，现已出狱并恢复收租" : " 休息一回合"));
        BoardEffect.cue(room,jailed ? actor.jailed ? "jail" : "release" : "rest",actor); if(!content.crossover.beforeEnd(room,actor)) advance(room); return true;
    }
}
