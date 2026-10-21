package com.dfw.domain;

import java.util.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import static com.dfw.domain.GameSupport.*;

/** 仅供客户端演出的公开效果事实，不参与业务结算或随机抽取。 */
@Getter
@Setter
public class BoardEffect {
    /** 演出类型，仅描述公开表现，不参与规则分支。 */
    public String kind;
    /** 参与演出的公开玩家编号，不包含手牌种类或数量。 */
    public List<String> playerIds = new ArrayList<>();
    /** 同时发生的玩家位移。 */
    public List<Relocation> moves = new ArrayList<>();
    /** 效果发生前的地产外观。 */
    public List<TileState> beforeTiles = new ArrayList<>();
    /** 效果发生后的地产外观。 */
    public List<TileState> afterTiles = new ArrayList<>();

    /** 一次直接位移，不增加沿途或落点结算。 */
    @Getter
    @Setter
    public static class Relocation {
        /** 位移玩家。 */
        public String playerId;
        /** 原位置。 */
        public int from;
        /** 目标位置。 */
        public int to;
    }

    /** 第一批效果涉及的地产公开外观快照，不含现金或私人手牌。 */
    @Getter
    @Setter
    @EqualsAndHashCode
    public static class TileState {
        /** 地产编号。 */
        public int id;
        /** 实际产权归属，用于取得地产演出的前后外观。 */
        public String ownerId;
        /** 建筑等级。 */
        public int level;
        /** 普通路障。 */
        public boolean barrier;
        /** 拆除护盾。 */
        public boolean demolitionShield;
        /** 防拆护盾剩余层数。 */
        public int demolitionShieldCharges;
        /** 宝藏标记。 */
        public boolean treasure;
        /** 演唱会层数。 */
        public int concertStacks;
        /** 陨星层数。 */
        public int meteorCharges;
        /** 神威施术者。 */
        public String kamuiCasterId;
        /** 神威剩余个人回合。 */
        public int kamuiTurns;
        /** 神威施加回合。 */
        public int kamuiAppliedTurn;
    }

    /** 输入类型和作用者，返回未发布的展示片段，不推进业务或消耗游戏随机源。 */
    public static MotionSegment begin(String kind, Player actor) {
        MotionSegment segment = new MotionSegment(); segment.id = UUID.randomUUID().toString();
        segment.playerId = actor.id; segment.from = actor.position; segment.teleport = true;
        segment.boardEffect = new BoardEffect(); segment.boardEffect.kind = kind;
        segment.boardEffect.playerIds.add(actor.id);
        return segment;
    }
    /** 输入目标地产，返回独立的公开外观快照，后续业务写入不会改动该快照。 */
    public static TileState appearance(Tile tile) {
        TileState state = new TileState(); state.id = tile.id; state.ownerId = tile.ownerId; state.level = tile.level;
        state.barrier = tile.barrier; state.demolitionShield = tile.demolitionShield; state.demolitionShieldCharges = PropertyEffects.shieldCharges(tile); state.treasure = tile.treasure;
        state.concertStacks = tile.concertStacks; state.meteorCharges = tile.meteorCharges;
        state.kamuiCasterId = tile.kamuiCasterId; state.kamuiTurns = tile.kamuiTurns; state.kamuiAppliedTurn = tile.kamuiAppliedTurn;
        return state;
    }
    /** 输入片段、玩家和实际终点，仅记录位移，不修改玩家或结算路径。 */
    public static void movement(MotionSegment segment, Player player, int destination) {
        Relocation move = new Relocation(); move.playerId = player.id; move.from = player.position; move.to = destination;
        segment.boardEffect.moves.add(move);
    }
    /** 输入房间及已完成事实，追加到现有路径队列；不重复抽取目标或执行效果。 */
    public static void publish(Room room, MotionSegment segment) {
        room.movementSequence.add(segment);
        refresh(room);
    }
    /** 输入房间，按最后一段重建兼容路径外壳，仅同步展示队列，无返回值。 */
    private static void refresh(Room room) {
        MotionSegment last = room.movementSequence.getLast();
        Motion motion = new Motion(); motion.id = last.id; motion.playerId = last.playerId;
        motion.from = last.from; motion.path = last.path; motion.teleport = last.teleport; motion.segments = new ArrayList<>(room.movementSequence);
        room.motion = motion;
    }
    /** 输入房间、类型和作用者，返回只读采样上下文，由调用者在原业务完成后发布。 */
    public static Observation watch(Room room, String kind, Player actor) { return new Observation(room, kind, actor); }
    /** 输入已确定效果及公开参与者，仅追加无金额、无手牌明细的演出，无返回值。 */
    public static void cue(Room room, String kind, Player actor, Player... participants) { watch(room, kind, actor).participants(Arrays.asList(participants)).finish(room); }

    /** 输入房间及原子效果，返回对应演出采样；未覆盖或已有专属记录的效果返回空。 */
    public static Observation watch(Room room, GameEffect effect) {
        String kind = switch (effect.type) {
            case "ANIME_PART" -> "kamui".equals(effect.selector) ? "kamui" : null;
            case "PLACE_BARRIER" -> "barrier";
            case "DEMOLISH", "DESTROY_BUILDINGS" -> "demolish";
            case "GRANT_MONEY" -> "income";
            case "GRANT_RANDOM_LAND" -> "talent";
            case "DRAW_CARDS" -> "supply";
            case "PAY_FEE", "CHARGE_GLOBAL_TARGET" -> "fee";
            case "SETTLE_PAYMENT" -> "payment";
            case "WAIVE_RENT" -> "immunity";
            case "SAP_STEAL" -> "steal";
            case "SETTLE_TILE" -> "landing";
            case "DIRECTED_TELEPORT", "RANDOM_TELEPORT", "SEND_TO_JAIL" -> "teleport";
            case "REPLACE_ROLL" -> "movement";
            case "MOVE" -> effect.content == null ? null : "movement";
            case "ADD_STATUS" -> switch (String.valueOf(effect.statusId)) {
                case "NEXT_ROLL_HALF" -> "slow";
                case "NEXT_MOVE_DOUBLE" -> "morale";
                case "TRAP_IMMUNITY" -> "trapImmune";
                case "NEXT_PAYMENT_WAIVER" -> "waiver";
                default -> "rentDouble";
            };
            case "CONTENT_EVENT" -> switch (effect.content.mechanism) {
                case "cards", "cards_one", "cards_two", "directed", "all_cards" -> "supply";
                case "treasure" -> "treasure";
                case "concert" -> "concert";
                default -> null;
            };
            default -> null;
        };
        Player actor = player(room, effect.playerId);
        if (kind == null || actor == null || actor.bankrupt) return null;
        Observation observation = watch(room, kind, actor);
        observation.source = effect.content;
        observation.prefix = Set.of("teleport", "movement").contains(kind);
        observation.force = Set.of("supply", "movement", "teleport", "steal", "immunity").contains(kind) || effect.type.equals("ADD_STATUS");
        if (effect.type.equals("SAP_STEAL")) { Player target = player(room,effect.targetId); observation.force = target != null && !target.bankrupt && !target.hand.isEmpty(); }
        if (effect.type.equals("DRAW_CARDS")) observation.force = effect.amount > 0;
        if (effect.targetId != null) observation.segment.boardEffect.playerIds.add(effect.targetId);
        if (effect.content != null && effect.content.mechanism.equals("all_cards")) observation.participants(room.players);
        return observation;
    }

    /** 一次原业务调用的公开外观采样；内部比较信息不会进入快照或网络。 */
    public static final class Observation {
        private static final Set<String> TILE_KINDS = Set.of("barrier", "barrierHit", "demolish", "shield", "exchange", "upgrade", "talent", "treasure", "concert", "concertRent", "landing", "kamui");
        private final MotionSegment segment;
        private final Map<Integer, TileState> tiles = new LinkedHashMap<>();
        private final Map<String, String> players = new LinkedHashMap<>();
        private final int index;
        private ContentResult source;
        private boolean prefix;
        private boolean force;

        /** 输入房间、展示类型和作用者，复制公开外观，不持有可变业务对象。 */
        private Observation(Room room, String kind, Player actor) {
            segment = begin(kind, actor); index = room.movementSequence.size();
            if (TILE_KINDS.contains(kind)) room.board.forEach(tile -> tiles.put(tile.id, appearance(tile)));
            room.players.forEach(player -> players.put(player.id, signature(player)));
        }
        /** 输入公开玩家，返回状态比较值；不读取手牌、候选或随机结果。 */
        private static String signature(Player player) {
            return player.cash + ":" + player.position + ":" + player.skipTurns + ":" + player.jailed + ":" + player.handLimitReduction + ":" + player.statuses.stream().map(status -> status.type + ":" + status.remainingTurns + ":" + status.appliedTurn).toList();
        }
        /** 输入实际参与者，添加公开编号并允许无外观变化的私人补给演出，返回当前采样。 */
        public Observation participants(List<Player> participants) {
            participants.stream().filter(player -> !player.bankrupt).forEach(player -> segment.boardEffect.playerIds.add(player.id));
            force = true; return this;
        }
        /** 输入房间，在原业务成功后记录实际变化；不执行校验、随机或资金操作，无返回值。 */
        public void finish(Room room) {
            BoardEffect effect = segment.boardEffect;
            boolean changed = false;
            for (Player player : room.players) if (!Objects.equals(players.get(player.id), signature(player))) {
                effect.playerIds.add(player.id); changed = true;
            }
            if (!tiles.isEmpty()) for (Tile tile : room.board) {
                TileState before = tiles.get(tile.id), after = appearance(tile);
                if (before != null && !before.equals(after)) { effect.beforeTiles.add(before); effect.afterTiles.add(after); changed = true; }
            }
            if (effect.kind.equals("landing")) {
                if (effect.beforeTiles.stream().noneMatch(tile -> tile.treasure && !room.board.get(tile.id).treasure)) return;
                effect.kind = "treasureClaim";
            }
            if (effect.kind.equals("fee") && (room.pending == null || !Set.of("rent", "debt").contains(room.pending.type))) return;
            if (effect.kind.equals("fee")) changed = true;
            if (!changed && !force) return;
            if (effect.kind.equals("demolish") && !effect.beforeTiles.isEmpty() && effect.beforeTiles.stream().allMatch(tile -> tile.level == room.board.get(tile.id).level)) effect.kind = "shieldBlock";
            effect.playerIds = new ArrayList<>(new LinkedHashSet<>(effect.playerIds));
            if (source != null && !source.privateSupply && !"card".equals(source.pool)) segment.beforeEvent = source;
            if (prefix) { room.movementSequence.add(index, segment); refresh(room); }
            else if (effect.kind.equals("kamui") && !room.movementSequence.isEmpty()
                    && room.movementSequence.getLast().boardEffect != null && room.movementSequence.getLast().boardEffect.kind.equals("kamui")
                    && Objects.equals(room.movementSequence.getLast().playerId, segment.playerId)) {
                BoardEffect previous = room.movementSequence.getLast().boardEffect;
                previous.beforeTiles.addAll(effect.beforeTiles); previous.afterTiles.addAll(effect.afterTiles);
                previous.playerIds = new ArrayList<>(new LinkedHashSet<>(java.util.stream.Stream.concat(previous.playerIds.stream(), effect.playerIds.stream()).toList()));
                refresh(room);
            } else if (Set.of("income", "supply", "morale", "trapImmune").contains(effect.kind) && !room.movementSequence.isEmpty()
                    && room.movementSequence.getLast().boardEffect != null && room.movementSequence.getLast().boardEffect.kind.equals(effect.kind)
                    && room.movementSequence.getLast().event == null && segment.beforeEvent == null) {
                BoardEffect previous = room.movementSequence.getLast().boardEffect;
                previous.playerIds = new ArrayList<>(new LinkedHashSet<>(java.util.stream.Stream.concat(previous.playerIds.stream(), effect.playerIds.stream()).toList()));
                refresh(room);
            } else publish(room, segment);
        }
    }
}
