package com.dfw.application;

import com.dfw.domain.*;
import com.dfw.domain.rules.RuleSet;
import com.dfw.interfaces.vo.*;
import com.dfw.infrastructure.JsonCodec;
import com.dfw.infrastructure.SessionTokens;
import com.dfw.application.RoomRepository.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionTemplate;

/** 按房间串行编排命令、连接和短事务，提交后才更新内存与推送。 */
@Service
public class RoomService {
    private static final Logger LOG = LoggerFactory.getLogger(RoomService.class);
    private final RoomRepository repository;
    private final StatePublisher publisher;
    private final JsonCodec json;
    private final SessionTokens tokens;
    private final RandomSource random;
    private final GameEngine engine;
    private final GameWorkflow workflow;
    private final TransactionTemplate transaction;
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();
    private volatile boolean closing;
    /** 输入持久化与联机端口，批量恢复房间并重置连接状态。 */
    public RoomService(RoomRepository repository, StatePublisher publisher, JsonCodec json, SessionTokens tokens, RandomSource random,
            GameWorkflow workflow, TransactionTemplate transaction, RuleSet rules, EconomyRules economyRules, MapRegions regions) {
        this.repository = repository; this.publisher = publisher; this.json = json; this.tokens = tokens; this.random = random;
        this.workflow = workflow; this.transaction = transaction; engine = new GameEngine(random, rules, economyRules, regions);
        List<Room> restored = repository.loadRooms(); workflow.verifyRecovery(restored);
        for (Room room : restored) {
            engine.restoreContentState(room); engine.refreshRuleDescriptions(room);
            for (Player player : room.players) player.online = false;
            engine.turns.refresh(room); rooms.put(room.code, room);
        }
    }
    /** 输入房间号和回调，在此房间的独立锁内执行并返回结果。 */
    private <T> T serial(String code, Supplier<T> operation) {
        ReentrantLock lock = locks.computeIfAbsent(code, key -> new ReentrantLock()); lock.lock();
        try { return operation.get(); } finally { lock.unlock(); }
    }
    /** 输入房间号，返回已存在状态，否则拒绝。 */
    private Room room(String code) { Room value = rooms.get(code); if (value == null) throw new GameError("房间不存在，请核对房间号", "ROOM_NOT_FOUND"); return value; }
    /** 输入候选房间和可选会话，保存后替换内存和广播。 */
    private void commit(Room value, StoredSession session) {
        value.version++;
        transaction.executeWithoutResult(status -> { workflow.refresh(value, engine); repository.save(value, session, null); });
        rooms.put(value.code, value); publisher.broadcast(value);
    }
    /** 输入昵称，创建唯一房间和首位玩家会话。 */
    public synchronized SessionVo create(String name) {
        String code; do { code = String.valueOf(100000 + random.nextInt(900000)); } while (rooms.containsKey(code));
        Player player = engine.createPlayer(name, 0); Room value = engine.createRoom(code, player); SessionVo session = issue(code, player.id);
        repository.save(value, stored(session), null); rooms.put(code, value); return session;
    }
    /** 输入房间号和昵称，串行加入大厅席位。 */
    public SessionVo join(String code, String name) {
        return serial(code, () -> {
            Room value = json.copy(room(code), Room.class);
            if (!value.status.equals("lobby")) throw new GameError("本局已开始，请进入观战", "GAME_STARTED");
            if (value.players.size() >= Rules.MAX_PLAYERS) throw new GameError("房间已满，最多六名玩家", "ROOM_FULL");
            Set<String> used = new HashSet<>(); value.players.forEach(p -> used.add(p.color)); int index = 0; while (used.contains(Rules.COLORS[index])) index++;
            Player player = engine.createPlayer(name, index); value.players.add(player); SessionVo session = issue(code, player.id); commit(value, stored(session)); return session;
        });
    }
    /** 输入房间号，保存只读观战身份。 */
    public SessionVo spectate(String code) { return serial(code, () -> { room(code); SessionVo session = issue(code, null); repository.addSpectator(stored(session)); return session; }); }
    /** 输入房间号及角色，签发会话令牌。 */
    private SessionVo issue(String code, String playerId) { SessionVo result = new SessionVo(); result.code = code; result.playerId = playerId; result.token = tokens.issue(); return result; }
    /** 输入浏览器会话，返回仅含摘要的持久化身份。 */
    private StoredSession stored(SessionVo value) { return new StoredSession(tokens.hash(value.token), value.code, value.playerId); }
    /** 输入连接及令牌，验证身份并提交在线状态后替换旧连接。 */
    public void authenticate(String connectionId, String token) {
        if (token == null || !token.matches("[a-f0-9]{64}")) throw new GameError("请求参数不正确", "INVALID_INPUT");
        StoredSession session = repository.session(tokens.hash(token)); if (session == null) throw new GameError("加入身份已失效，请重新加入");
        serial(session.code(), () -> {
            Room value = json.copy(room(session.code()), Room.class);
            if (session.playerId() != null) {
                Player player = GameSupport.player(value, session.playerId()); if (player == null) throw new GameError("你的席位已被移出");
                player.online = true; value.version++;
                transaction.executeWithoutResult(status -> { workflow.refresh(value, engine); repository.save(value, null, null); });
                rooms.put(value.code, value);
            }
            publisher.attach(connectionId, session); publisher.broadcast(value); publisher.sync(connectionId, value); return null;
        });
    }
    /** 输入连接，串行读取完整房间并按角色同步。 */
    public void sync(String connectionId) { StoredSession session = authenticated(connectionId); serial(session.code(), () -> { publisher.sync(connectionId, room(session.code())); return null; }); }
    /** 输入连接，返回认证身份或拒绝。 */
    private StoredSession authenticated(String connectionId) { StoredSession session = publisher.session(connectionId); if (session == null) throw new GameError("请先认证连接", "FORBIDDEN"); return session; }
    /** 输入连接、命令标识、预期版本和领域操作，返回幂等提交结果。 */
    public CommandResult command(String connectionId, String id, long expectedVersion, Action action) {
        StoredSession session = authenticated(connectionId);
        return serial(session.code(), () -> {
            if (session.playerId() == null || !publisher.active(connectionId, session)) throw new GameError("观战或已被替换的连接不能操作", "FORBIDDEN");
            String requestDigest = json.actionDigest(action);
            Receipt receipt = repository.receipt(session.code(), id);
            if (receipt != null) {
                if (!receipt.actorId().equals(session.playerId())) throw new GameError("命令身份不匹配", "FORBIDDEN");
                if (receipt.requestDigest() != null && !receipt.requestDigest().equals(requestDigest)) throw new GameError("命令标识已用于另一项操作，请重新操作", "INVALID_INPUT");
                return receipt.result();
            }
            Room original = room(session.code());
            if (expectedVersion != original.version) { publisher.sync(connectionId, original); throw new GameError("局面已更新，请查看后重新操作", "STALE_VERSION"); }
            Room value = json.copy(original, Room.class);
            CommandResult result = transaction.execute(status -> {
                workflow.apply(value, engine, session.playerId(), action); workflow.refresh(value, engine); value.version++;
                CommandResult committed = new CommandResult(); committed.ok = true; committed.version = value.version;
                repository.save(value, null, new Receipt(id, session.playerId(), committed, requestDigest)); return committed;
            });
            rooms.put(value.code, value); publisher.broadcast(value); return result;
        });
    }
    /** 输入关闭的连接，仅有效玩家连接提交离线状态。 */
    public void disconnected(String connectionId) {
        StoredSession session = publisher.session(connectionId);
        if (session == null) { publisher.remove(connectionId); return; }
        try {
            if (!closing) serial(session.code(), () -> {
                if (session.playerId() != null && publisher.active(connectionId, session)) {
                    Room value = json.copy(room(session.code()), Room.class); Player player = GameSupport.player(value, session.playerId());
                    if (player != null) { player.online = false; commit(value, null); }
                } return null;
            });
        } finally { publisher.remove(connectionId); }
    }
    /** 每秒按有效连接注册修复断线保存失败的在线标记，保留玩家尚未回应的报价。 */
    @Scheduled(fixedDelay = 1000)
    public void refreshDisconnectedPlayers() {
        if (closing) return;
        for (Room value : rooms.values()) {
            if (value.players.stream().anyMatch(p -> p.online && !publisher.online(value.code, p.id))) {
                try { serial(value.code, () -> {
                    Room candidate = json.copy(room(value.code), Room.class); boolean changed = false;
                    for (Player p : candidate.players) if (p.online && !publisher.online(candidate.code, p.id)) { p.online = false; changed = true; }
                    if (changed) commit(candidate, null); return null;
                }); } catch (RuntimeException error) { LOG.error("离线状态恢复保存失败，房间 {}", value.code, error); }
            }
        }
    }
    /** 停止新推进，按房间保存离线状态及待处理报价后关闭连接。 */
    @PreDestroy
    public void shutdown() {
        closing = true;
        for (String code : rooms.keySet()) try { serial(code, () -> {
            Room value = json.copy(room(code), Room.class); value.players.forEach(p -> p.online = false); commit(value, null); return null;
        }); } catch (RuntimeException error) { LOG.error("停机保存失败，房间 {}", code, error); }
        publisher.closeAll();
    }
}
