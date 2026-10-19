package com.dfw.application;

import com.dfw.domain.*;
import com.dfw.domain.rules.RuleSet;
import com.dfw.interfaces.vo.*;
import com.dfw.interfaces.StateViews;
import com.dfw.infrastructure.JsonCodec;
import com.dfw.infrastructure.SessionTokens;
import com.dfw.application.RoomRepository.*;
import com.dfw.interfaces.dto.ArchiveInput;
import com.dfw.interfaces.dto.RoomLogInput;
import com.dfw.interfaces.dto.SeatClaimInput;
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
    private final ArchiveRepository archives;
    private final StatePublisher publisher;
    private final JsonCodec json;
    private final SessionTokens tokens;
    private final RandomSource random;
    private final GameEngine engine;
    private final GameWorkflow workflow;
    private final TransactionTemplate transaction;
    private final BotDialogueService botDialogue;
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();
    /** 各机器人失败的等待点；聊天或连接更新不清除保守兜底。 */
    private final Map<String, String> botFallbackDecisions = new ConcurrentHashMap<>();
    private volatile boolean closing;
    /** 输入持久化与联机端口，批量恢复房间；真人等待重连，机器人恢复服务端在线状态。 */
    public RoomService(RoomRepository repository, StatePublisher publisher, JsonCodec json, SessionTokens tokens, RandomSource random,
            GameWorkflow workflow, TransactionTemplate transaction, RuleSet rules, EconomyRules economyRules, MapRegions regions, ArchiveRepository archives,
            BotDialogueService botDialogue) {
        this.repository = repository; this.publisher = publisher; this.json = json; this.tokens = tokens; this.random = random;
        this.archives = archives;
        this.botDialogue = botDialogue;
        this.workflow = workflow; this.transaction = transaction; engine = new GameEngine(random, rules, economyRules, regions);
        List<Room> restored = repository.loadRooms(); workflow.verifyRecovery(restored); archives.initialize(restored);
        for (Room room : restored) {
            engine.restoreContentState(room); engine.refreshRuleDescriptions(room);
            for (Player player : room.players) player.online = player.botDifficulty != null;
            engine.turns.refresh(room); rooms.put(room.code, room);
        }
    }
    /** 输入房间号和回调，在此房间的独立锁内执行并返回结果。 */
    private <T> T serial(String code, Supplier<T> operation) {
        ReentrantLock lock = locks.computeIfAbsent(code, key -> new ReentrantLock()); lock.lock();
        try { return HistoryTrace.capture(operation); } finally { lock.unlock(); }
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
        return HistoryTrace.capture(() -> {
            String code = newRoomCode(); Player player = engine.createPlayer(name, 0); Room value = engine.createRoom(code, player); archives.identify(value); SessionVo session = issue(value, player.id);
            HistoryTrace.event(value, "create", player.id, "创建房间", null);
            repository.save(value, stored(session), null); rooms.put(code, value); session.archiveCode = value.archiveCode; return session;
        });
    }
    /** 输入房间号和昵称，串行加入大厅席位。 */
    public SessionVo join(String code, String name) {
        return serial(code, () -> {
            Room value = json.copy(room(code), Room.class);
            if (value.recovered && (value.recoveryPending || !value.status.equals("lobby") || value.players.size() >= Rules.MAX_PLAYERS)) { SessionVo visitor = issue(value, null); visitor.visitor = true; HistoryTrace.event(value, "visitor_join", null, "访客进入恢复房间", null); commit(value, stored(visitor)); return visitor; }
            if (!value.status.equals("lobby")) throw new GameError("本局已开始，请进入观战", "GAME_STARTED");
            if (value.players.size() >= Rules.MAX_PLAYERS) throw new GameError("房间已满，最多六名玩家", "ROOM_FULL");
            Set<String> used = new HashSet<>(); value.players.forEach(p -> used.add(p.color)); int index = 0; while (used.contains(Rules.COLORS[index])) index++;
            Player player = engine.createPlayer(name, index); value.players.add(player); SessionVo session = issue(value, player.id); HistoryTrace.event(value, "join", player.id, "加入房间", null); commit(value, stored(session)); return session;
        });
    }
    /** 输入房间号，保存只读观战身份。 */
    public SessionVo spectate(String code) { return serial(code, () -> { Room value = room(code); SessionVo session = issue(value, null); repository.addSpectator(stored(session)); return session; }); }
    /** 输入房间号及角色，签发会话令牌。 */
    private SessionVo issue(Room room, String playerId) { SessionVo result = new SessionVo(); result.code = room.code; result.roomId = room.roomId; result.archiveCode = playerId == null ? null : room.archiveCode; result.playerId = playerId; result.token = tokens.issue(); return result; }
    /** 输入浏览器会话，返回仅含摘要的持久化身份。 */
    private StoredSession stored(SessionVo value) { return new StoredSession(tokens.hash(value.token), value.code, value.playerId, value.roomId); }
    /** 输入连接及令牌，验证身份并提交在线状态后替换旧连接。 */
    public void authenticate(String connectionId, String token) {
        if (token == null || !token.matches("[a-f0-9]{64}")) throw new GameError("请求参数不正确", "INVALID_INPUT");
        StoredSession session = validSession(token);
        serial(session.code(), () -> {
            verifySession(session);
            Room value = json.copy(room(session.code()), Room.class);
            if (session.playerId() != null) {
                Player player = GameSupport.player(value, session.playerId()); if (player == null) throw new GameError("你的席位已被移出");
                player.online = true; value.version++;
                HistoryTrace.event(value, "connected", player.id, "角色已连接", null);
                transaction.executeWithoutResult(status -> { workflow.refresh(value, engine); repository.save(value, null, null); });
                rooms.put(value.code, value);
            }
            publisher.attach(connectionId, session); publisher.identity(connectionId, value); publisher.broadcast(value); publisher.sync(connectionId, value); return null;
        });
    }
    /** 输入连接，串行读取完整房间并按角色同步。 */
    public void sync(String connectionId) { StoredSession session = authenticated(connectionId); serial(session.code(), () -> { publisher.sync(connectionId, room(session.code())); return null; }); }
    /** 输入连接，返回认证身份或拒绝。 */
    private StoredSession authenticated(String connectionId) { StoredSession session = publisher.session(connectionId); if (session == null || !Objects.equals(room(session.code()).roomId, session.roomId())) throw new GameError("请先认证连接", "FORBIDDEN"); return session; }
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
            boolean activityInput = ActivityService.isAction(action.type) && expectedVersion <= original.version;
            if (expectedVersion != original.version && !activityInput) { publisher.sync(connectionId, original); throw new GameError("局面已更新，请查看后重新操作", "STALE_VERSION"); }
            Room value = json.copy(original, Room.class);
            StoredSession[] assigned = new StoredSession[1];
            CommandResult result = transaction.execute(status -> {
                if (!action.type.equals("chat") && !action.type.equals("setName")) HistoryTrace.event(value, action.type, session.playerId(), actionText(action.type), action);
                if (action.type.equals("assignSeat")) assigned[0] = assign(value, session.playerId(), action.claimId);
                else if (action.type.equals("continueRecovery")) {
                    if (!value.recoveryPending || !Objects.equals(value.hostId, session.playerId())) throw new GameError("只有房主可继续恢复对局");
                    value.recoveryPending = false;
                } else {
                    if (value.recoveryPending) throw new GameError("请等待房主分配角色并继续对局");
                    workflow.apply(value, engine, session.playerId(), action);
                }
                workflow.refresh(value, engine); value.version++;
                CommandResult committed = new CommandResult(); committed.ok = true; committed.version = value.version;
                repository.save(value, null, new Receipt(id, session.playerId(), committed, requestDigest)); return committed;
            });
            rooms.put(value.code, value); if (assigned[0] != null) publisher.assign(value, assigned[0]); publisher.broadcast(value); return result;
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
                    if (player != null) { player.online = player.botDifficulty != null; HistoryTrace.event(value, "disconnected", player.id, "角色已断线", null); commit(value, null); }
                } return null;
            });
        } finally { publisher.remove(connectionId); }
    }
    /** 每秒按有效连接修复真人在线标记，机器人无需浏览器连接，保留尚未回应的报价。 */
    @Scheduled(fixedDelay = 1000)
    public void refreshDisconnectedPlayers() {
        if (closing) return;
        for (Room value : rooms.values()) {
            if (value.players.stream().anyMatch(p -> p.botDifficulty == null && p.online && !publisher.online(value.roomId, p.id))) {
                try { serial(value.code, () -> {
                    Room candidate = json.copy(room(value.code), Room.class); boolean changed = false;
                    for (Player p : candidate.players) if (p.botDifficulty == null && p.online && !publisher.online(candidate.roomId, p.id)) { p.online = false; changed = true; HistoryTrace.event(candidate, "disconnected", p.id, "连接状态已修复为离线", null); }
                    if (changed) commit(candidate, null); return null;
                }); } catch (RuntimeException error) { LOG.error("离线状态恢复保存失败，房间 {}", value.code, error); }
            }
        }
    }
    /** 每250毫秒推进已到期的活动展示；先检查内存，仅到期房间进入独立事务。 */
    @Scheduled(fixedDelay = 250)
    public void refreshActivities() {
        if (closing) return;
        for (Room value : rooms.values()) if (value.status.equals("playing") && !value.recoveryPending && engine.activities.due(value)) {
            try { serial(value.code, () -> { Room latest = room(value.code); if (engine.activities.due(latest)) commit(json.copy(latest, Room.class), null); return null; }); }
            catch (RuntimeException error) { LOG.error("活动展示推进失败，房间 {}", value.code, error); }
        }
    }
    /** 每八百毫秒按房间选择一个机器人意图，复用流程事务与规则；等待真人时保持原等待规则。 */
    @Scheduled(fixedDelay = 800)
    public void advanceBots() {
        if (closing) return;
        for (Room snapshot : rooms.values()) {
            if (!snapshot.status.equals("playing") || snapshot.recoveryPending || snapshot.pausedReason != null || snapshot.players.stream().noneMatch(p -> p.botDifficulty != null && !p.bankrupt)) {
                botDialogue.suspend(snapshot); continue;
            }
            try { serial(snapshot.code, () -> {
                Room latest = room(snapshot.code);
                if (closing || !latest.status.equals("playing") || latest.recoveryPending || latest.pausedReason != null) return null;
                BotDialogueService.Poll dialogue = botDialogue.poll(latest);
                if (dialogue.waiting()) return null;
                if (dialogue.selection() != null) {
                    BotDialogueService.Selection selected = dialogue.selection();
                    try {
                        if (selected.action() != null || selected.message() != null) applyBot(latest, selected.botId(), selected.action(), selected.message(), selected.confirmed());
                        botDialogue.applied(selected, selected.action() == null && selected.message() == null ? "silent" : "committed");
                        if (selected.action() != null) botFallbackDecisions.remove(selected.botId());
                    } catch (RuntimeException error) {
                        boolean persisted = room(latest.code).version > latest.version;
                        if (!persisted && selected.action() != null) botFallbackDecisions.put(selected.botId(), latest.gameId + ":" + latest.runtime.decisionKey);
                        botDialogue.applied(selected, persisted ? "committed_broadcast_failed" : "commit_failed");
                        LOG.warn("机器人对话或议价处理失败，房间 {}，是否已提交 {}", latest.code, persisted);
                    }
                    return null;
                }
                if (latest.activity != null && latest.activity.endsAt != null && latest.pending == null) return null;
                if (botDialogue.chat(latest)) return null;
                String required = latest.trade != null ? latest.trade.toId : latest.pending != null ? latest.pending.playerId : latest.activity == null ? GameSupport.current(latest).id : null;
                PublicRoomVo visible = StateViews.publicRoom(latest);
                for (Player bot : latest.players) if (bot.botDifficulty != null && !bot.bankrupt && (required == null || required.equals(bot.id))) {
                    String decision = latest.gameId + ":" + latest.runtime.decisionKey;
                    boolean conservative = Objects.equals(botFallbackDecisions.get(bot.id), decision);
                    Action action;
                    BotPlanner planner;
                    try {
                        planner = new BotPlanner(visible, StateViews.privatePlayer(latest, bot.id), bot.botTradeQuotes, conservative);
                        long alive = latest.players.stream().filter(p -> !p.bankrupt).count();
                        action = planner.next(bot.botTradeTurn >= 0 && latest.turnNumber - bot.botTradeTurn <= alive);
                    } catch (RuntimeException error) {
                        botFallbackDecisions.put(bot.id, decision);
                        LOG.error("机器人规划失败，下一次使用保守操作，房间 {}，玩家 {}", latest.code, bot.id, error);
                        break;
                    }
                    if (action == null) continue;
                    if (!conservative && botDialogue.trade(latest, bot, action, planner)) break;
                    try { applyBot(latest, bot.id, action); botFallbackDecisions.remove(bot.id); }
                    catch (GameError error) {
                        // 失败事务的事实随本轮 capture 丢弃，下一轮再提交兜底，避免历史混入未发生操作。
                        botFallbackDecisions.put(bot.id, decision);
                        LOG.warn("机器人操作被规则拒绝，房间 {}，玩家 {}，操作 {}：{}", latest.code, bot.id, action.type, error.getMessage());
                    }
                    break;
                }
                return null;
            }); } catch (RuntimeException error) { LOG.error("机器人推进失败，房间 {}", snapshot.code, error); }
        }
    }
    /** 输入最新房间、机器人身份与意图，在房间锁内提交单步行动，失败不替换正式快照。 */
    private void applyBot(Room latest, String actorId, Action action) {
        applyBot(latest, actorId, action, null, false);
    }
    /** 输入行动和安全话术，在同一事务内提交交易、确认标记与聊天；未提交交易时绝不广播承诺。 */
    private void applyBot(Room latest, String actorId, Action action, String message, boolean confirmed) {
        Room value = json.copy(latest, Room.class);
        String quote = action != null && action.type.equals("tradeOffer") ? BotPlanner.quoteKey(StateViews.publicRoom(latest), action) : null;
        transaction.executeWithoutResult(status -> {
            if (action != null) {
                HistoryTrace.event(value, action.type, actorId, actionText(action.type), action);
                workflow.apply(value, engine, actorId, action);
            }
            if (confirmed && quote != null) {
                if (value.trade == null || !actorId.equals(value.trade.fromId)) throw new IllegalStateException("报价未建立，不能确认成交条件");
                value.trade.botConfirmed = true;
            }
            if (message != null) {
                Action speech = new Action(); speech.type = "chat";
                speech.message = message.length() <= 300 ? message : confirmed ? "我确认这份交易条件，正式报价已发出，请在交易面板核对完整明细后确认。" : "这组条件还需讨论，请在交易面板发出明确报价，当前没有成交。";
                workflow.apply(value, engine, actorId, speech);
            }
            if (quote != null) {
                Player bot = GameSupport.player(value, actorId); bot.botTradeTurn = value.turnNumber;
                bot.botTradeQuotes.put(quote, value.turnNumber);
                if (bot.botTradeQuotes.size() > 32) bot.botTradeQuotes.remove(bot.botTradeQuotes.keySet().iterator().next());
            }
            workflow.refresh(value, engine); value.version++;
            repository.save(value, null, null);
        });
        rooms.put(value.code, value); publisher.broadcast(value);
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
    /** 返回本进程未使用的六位入房号；首期不回收已有号码。 */
    private String newRoomCode() { String code; do { code = String.valueOf(100000 + random.nextInt(900000)); } while (rooms.containsKey(code)); return code; }
    /** 输入原始令牌，确认数据库身份和永久房间归属，返回有效会话。 */
    private StoredSession validSession(String token) {
        if (token == null || !token.matches("[a-f0-9]{64}")) throw new GameError("身份已失效，请重新进入房间", "FORBIDDEN");
        StoredSession session = repository.session(tokens.hash(token));
        if (session == null || !Objects.equals(room(session.code()).roomId, session.roomId())) throw new GameError("加入身份已失效，请重新加入", "FORBIDDEN"); return session;
    }
    /** 输入房间会话和游标，返回本局完整公开动态，玩家与观战者均可查阅。 */
    public HistoryPageVo publicLogs(RoomLogInput input) {
        StoredSession session = validSession(input.token);
        if (input.cursor != null && input.cursor < 0) throw new GameError("记录游标不可用");
        return serial(session.code(), () -> {
            verifySession(session); Room value = room(session.code());
            ArchiveInput query = new ArchiveInput(); query.archiveCode = value.archiveCode;
            query.gameId = value.gameId == null ? "lobby" : value.gameId; query.type = "live"; query.cursor = input.cursor;
            return archives.history(query);
        });
    }
    /** 输入玩家会话、版本和备注，原子标记当前已提交保存点，不推进游戏。 */
    public ArchiveSnapshotVo saveArchive(ArchiveInput input) {
        StoredSession session = validSession(input.token);
        return serial(session.code(), () -> {
            verifySession(session);
            Room value = room(session.code());
            if (session.playerId() == null || GameSupport.player(value, session.playerId()) == null) throw new GameError("请使用玩家身份存档", "FORBIDDEN");
            if (input.expectedVersion == null || input.expectedVersion != value.version) throw new GameError("局面已更新，请同步后存档", "STALE_VERSION");
            return transaction.execute(status -> archives.mark(value, input.label, session.playerId()));
        });
    }
    /** 输入共享码、保存点和去重请求，新建恢复房间或返回同一请求已创建的房间。 */
    public synchronized SessionVo restoreArchive(ArchiveInput input) {
        ArchiveRepository.SavedRoom saved = archives.saved(input.archiveCode, input.snapshotId);
        String digest = json.actionDigest(restoreAction(input));
        ArchiveRepository.RestoredRoom previous = archives.restored(saved.roomId(), input.requestId);
        if (previous != null) {
            if (!Objects.equals(previous.digest(), digest)) throw new GameError("恢复请求标识已用于其他保存点，请重新选择", "INVALID_INPUT");
            return serial(previous.code(), () -> {
                Room value = room(previous.code()); if (GameSupport.player(value, previous.playerId()) == null) throw new GameError("恢复者的角色已移出，请申请原房间角色");
                SessionVo session = issue(value, previous.playerId()); session.token = tokens.restored(saved.room().archiveCode, input.requestId, value.roomId);
                StoredSession stored = repository.session(tokens.hash(session.token));
                if (stored == null || !Objects.equals(stored.roomId(), value.roomId) || !Objects.equals(stored.playerId(), previous.playerId())) throw new GameError("恢复者身份已交接，请进入恢复房间申请角色", "FORBIDDEN");
                return session;
            });
        }
        if (saved.restoreError() != null) throw new GameError(saved.restoreError(), "INCOMPATIBLE_ARCHIVE");
        return HistoryTrace.capture(() -> {
            Room value = saved.room(); String sourceRoomId = saved.roomId(); String sourceArchiveCode = value.archiveCode;
            value.code = newRoomCode(); value.roomId = null; value.archiveCode = null; archives.identify(value); value.sourceRoomId = sourceRoomId; value.sourceSnapshotId = saved.snapshotId();
            value.gameId = value.gameId == null ? null : random.id(); value.version = 1; value.recoveryPending = true; value.recovered = true; value.seatClaims.clear();
            value.players.forEach(player -> player.online = false);
            String playerId = input.playerId == null ? value.hostId : input.playerId;
            if (GameSupport.player(value, playerId) == null || GameSupport.player(value, playerId).botDifficulty != null) throw new GameError("请选择保存点中的角色", "INVALID_INPUT");
            value.hostId = playerId; engine.turns.refresh(value); SessionVo session = issue(value, playerId);
            session.token = tokens.restored(sourceArchiveCode, input.requestId, value.roomId);
            HistoryTrace.event(value, "restored", playerId, "从保存点创建恢复房间，等待分配角色", Map.of("sourceRoomId", sourceRoomId, "sourceSnapshotId", saved.snapshotId()));
            transaction.executeWithoutResult(status -> {
                workflow.restore(value, engine); repository.save(value, stored(session), null); archives.bindRestore(value, input.requestId, digest, playerId);
            });
            rooms.put(value.code, value); session.archiveCode = value.archiveCode; return session;
        });
    }
    /** 输入恢复资料，构造仅包含用户所选字段的稳定请求摘要，最新进度变化不改变重试语义。 */
    private Action restoreAction(ArchiveInput input) { Action action = new Action(); action.type = "restore"; action.decisionId = input.snapshotId; action.playerId = input.playerId; return action; }
    /** 输入访客身份和角色，在房间锁内保存申请，返回无令牌摘要的申请资料。 */
    public SeatClaimVo claim(SeatClaimInput input) {
        StoredSession session = validSession(input.token);
        return serial(session.code(), () -> {
            verifySession(session);
            Room value = json.copy(room(session.code()), Room.class);
            if (!value.recovered || session.playerId() != null) throw new GameError("请使用访客身份申请恢复房间角色");
            if (GameSupport.player(value, input.playerId) == null || GameSupport.player(value, input.playerId).botDifficulty != null) throw new GameError("请选择房间中的保存角色", "INVALID_INPUT");
            SeatClaim existingClaim = value.seatClaims.stream().filter(c -> c.tokenHash.equals(session.tokenHash()) && c.playerId.equals(input.playerId)).findFirst().orElse(null);
            if (existingClaim != null) return claimView(existingClaim);
            value.seatClaims.removeIf(c -> c.tokenHash.equals(session.tokenHash())); SeatClaim claim = new SeatClaim(); claim.id = random.id(); claim.tokenHash = session.tokenHash(); claim.playerId = input.playerId; claim.name = input.name == null || input.name.isBlank() ? "访客" : input.name.trim(); claim.at = System.currentTimeMillis(); value.seatClaims.add(claim);
            HistoryTrace.event(value, "seat_claim", null, "访客申请角色：" + GameSupport.player(value, claim.playerId).name, null); commit(value, null); return claimView(claim);
        });
    }
    /** 输入申请，返回只用于界面辨认的白名单字段。 */
    private SeatClaimVo claimView(SeatClaim claim) { SeatClaimVo value = new SeatClaimVo(); value.id = claim.id; value.playerId = claim.playerId; value.name = claim.name; value.at = claim.at; return value; }
    /** 输入锁外读取的身份，在房间锁内再次核对，防止分配期间已撤销的令牌继续认证。 */
    private void verifySession(StoredSession session) { if (!session.equals(repository.session(session.tokenHash()))) throw new GameError("身份已变更，请重新连接", "FORBIDDEN"); }
    /** 输入房主与申请，在现有事务中撤销旧席位身份并绑定访客，返回待同步的新会话。 */
    private StoredSession assign(Room room, String actorId, String claimId) {
        if (!room.recovered || !Objects.equals(actorId, room.hostId)) throw new GameError("只有恢复房间的房主可分配角色", "FORBIDDEN");
        SeatClaim claim = room.seatClaims.stream().filter(c -> c.id.equals(claimId)).findFirst().orElse(null);
        if (claim == null) throw new GameError("此申请已处理，请查看最新申请");
        StoredSession visitor = repository.session(claim.tokenHash); Player target = GameSupport.player(room, claim.playerId);
        if (visitor == null || visitor.playerId() != null || !Objects.equals(visitor.roomId(), room.roomId) || target == null || target.botDifficulty != null) throw new GameError("申请身份或角色已失效");
        repository.assignSeat(room, claim.tokenHash, claim.playerId); room.seatClaims.removeIf(c -> c.tokenHash.equals(claim.tokenHash)); target.online = publisher.connected(claim.tokenHash);
        HistoryTrace.event(room, "seat_assigned", target.id, "房主将角色分配给 " + claim.name, null);
        return new StoredSession(claim.tokenHash, room.code, target.id, room.roomId);
    }
    /** 输入操作类型，返回不包含手牌与私人报价参数的公开说明。 */
    private String actionText(String type) {
        return switch (type) {
            case "addBot" -> "添加机器人"; case "setBotDifficulty" -> "调整机器人难度";
            case "activityInput" -> "参与角落活动";
            case "ready" -> "更新准备状态"; case "setMapSize" -> "选择地图大小"; case "start" -> "开始对局"; case "newGame" -> "返回大厅，再开一局";
            case "roll" -> "掷骰行动"; case "endTurn" -> "结束行动"; case "card" -> "使用道具"; case "buy" -> "购买地产"; case "upgrade" -> "升级建筑"; case "pass" -> "放弃当前购买或升级";
            case "tradeOffer" -> "发起私人报价"; case "tradeRespond" -> "回应私人报价"; case "tradeCancel" -> "取消私人报价"; case "assignSeat" -> "批准角色申请"; case "continueRecovery" -> "继续恢复对局";
            case "mortgage" -> "抵押地产"; case "redeem" -> "赎回地产"; case "sellLand" -> "出售地产"; case "sellBuilding" -> "出售建筑"; case "removePlayer" -> "移出离线玩家";
            case "declareBankruptcy" -> "申请清算"; case "payDebt", "rentDecision", "paymentDefense" -> "处理费用结算"; case "contentDecision" -> "处理私人选择"; default -> "完成游戏操作";
        };
    }
}
