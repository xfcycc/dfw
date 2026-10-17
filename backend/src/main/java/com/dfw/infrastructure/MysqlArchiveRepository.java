package com.dfw.infrastructure;

import com.dfw.application.ArchiveRepository;
import com.dfw.domain.*;
import com.dfw.interfaces.StateViews;
import com.dfw.interfaces.dto.ArchiveInput;
import com.dfw.interfaces.vo.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.*;

/** InnoDB 追加历史与不可变完整快照；公开读取始终使用保存时的白名单投影。 */
@Repository
public class MysqlArchiveRepository implements ArchiveRepository {
    private static final String ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";
    private static final String SNAPSHOT_INSERT = "INSERT INTO room_snapshots(snapshot_id,room_id,game_id,version,turn_number,status,at,format_version,rules_digest,state,public_state) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
    private static final String HISTORY_INSERT = "INSERT INTO room_history(room_id,snapshot_id,game_id,version,turn_number,phase,actor_id,actor_name,type,text,at,details) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;
    private final JsonCodec json;
    private final ArchiveRules rules;
    private final SecureRandom random = new SecureRandom();
    private final Set<String> existing = ConcurrentHashMap.newKeySet();
    /** 输入存储、事务、编解码和摘要，等待增量列准备后使用档案表。 */
    public MysqlArchiveRepository(JdbcTemplate jdbc, TransactionTemplate transaction, JsonCodec json, ArchiveRules rules, ArchiveSchema schema) {
        this.jdbc = jdbc; this.transaction = transaction; this.json = json; this.rules = rules;
    }
    /** 输入房间，生成缺失稳定标识；存档码只有插入冲突时才重新生成。 */
    public void identify(Room room) { if (room.roomId == null) room.roomId = UUID.randomUUID().toString(); if (room.archiveCode == null) room.archiveCode = newCode(); }
    /** 返回二十六位随机编码，字符集排除易混淆字母。 */
    private String newCode() { StringBuilder code = new StringBuilder(); for (int i = 0; i < 26; i++) code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length()))); return code.toString(); }
    /** 输入房间，在当前事务插入新档案并以唯一索引处理随机码冲突。 */
    private void ensure(Room room) {
        if (existing.contains(room.roomId)) return;
        while (true) {
            try { jdbc.update("INSERT INTO room_archives(room_id,archive_code,code,source_room_id,source_snapshot_id) VALUES (?,?,?,?,?)", room.roomId, room.archiveCode, room.code, room.sourceRoomId, room.sourceSnapshotId); break; }
            catch (DuplicateKeyException error) { if (!error.getMessage().contains("uq_archive_code")) throw error; room.archiveCode = newCode(); }
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            /** 仅成功提交后缓存档案标识，失败不污染下一次重试。 */
            @Override public void afterCommit() { existing.add(room.roomId); }
        });
    }
    /** 输入已存在房间，一次读取档案标识，再批量建立缺失的当前基线与身份归属。 */
    public void initialize(List<Room> rooms) {
        existing.addAll(jdbc.query("SELECT room_id FROM room_archives", (rs, row) -> rs.getString(1)));
        List<Room> missing = rooms.stream().filter(room -> room.roomId == null || !existing.contains(room.roomId)).toList();
        if (missing.isEmpty()) return;
        transaction.executeWithoutResult(status -> {
            List<Object[]> states = new ArrayList<>(), sessions = new ArrayList<>(), snapshots = new ArrayList<>(), events = new ArrayList<>();
            for (Room room : missing) {
                identify(room); ensure(room);
                states.add(new Object[]{room.roomId, json.write(room), room.code}); sessions.add(new Object[]{room.roomId, room.code}); snapshots.add(snapshotRow(room));
                List<PublicLog> logs = new ArrayList<>(room.logs); Collections.reverse(logs);
                for (PublicLog log : logs) events.add(historyRow(room, new HistoryTrace.Entry(room.gameId, null, null, null, null, "legacy_log", log.text, log.at, null)));
                events.add(historyRow(room, new HistoryTrace.Entry(room.gameId, room.turnNumber, room.phase, null, null, "baseline", "开始记录历史；此前已丢失记录无法补回", System.currentTimeMillis(), null)));
            }
            jdbc.batchUpdate("UPDATE rooms SET room_id=?,state=? WHERE code=?", states);
            jdbc.batchUpdate("UPDATE sessions SET room_id=? WHERE code=?", sessions);
            jdbc.batchUpdate(SNAPSHOT_INSERT, snapshots); jdbc.batchUpdate(HISTORY_INSERT, events);
        });
    }
    /** 输入房间，根据内部标识与提交版本返回稳定保存点标识。 */
    private String snapshotId(Room room) { return UUID.nameUUIDFromBytes((room.roomId + ":" + room.version).getBytes(StandardCharsets.UTF_8)).toString(); }
    /** 输入已稳定候选，返回包含内部状态与当时公开局面的快照行。 */
    private Object[] snapshotRow(Room room) { return new Object[]{snapshotId(room), room.roomId, room.gameId, room.version, room.turnNumber, room.status, System.currentTimeMillis(), ArchiveRules.FORMAT, rules.digest, json.write(room), json.write(StateViews.publicRoom(room))}; }
    /** 输入候选与实际事实，返回批量写入参数；内部明细不作为公开接口输出。 */
    private Object[] historyRow(Room room, HistoryTrace.Entry entry) { return new Object[]{room.roomId, snapshotId(room), entry.gameId(), room.version, entry.turnNumber(), entry.phase(), entry.actorId(), entry.actorName(), entry.type(), entry.text(), entry.at(), entry.details() == null ? null : json.write(entry.details())}; }
    /** 输入候选，在外层事务中一次保存完整快照并批量追加本次事实。 */
    public void append(Room room) {
        identify(room); ensure(room); jdbc.update(SNAPSHOT_INSERT, snapshotRow(room));
        List<HistoryTrace.Entry> entries = HistoryTrace.entries();
        if (entries.isEmpty()) entries = List.of(new HistoryTrace.Entry(room.gameId, room.turnNumber, room.phase, null, null, "state", "局面已保存", System.currentTimeMillis(), null));
        jdbc.batchUpdate(HISTORY_INSERT, entries.stream().map(entry -> historyRow(room, entry)).toList());
    }
    /** 输入用户编码，兼容大小写和分组符号，拒绝无效编码。 */
    private String code(String value) {
        String normalized = value == null ? "" : value.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
        if (!normalized.matches("[0-9A-HJKMNP-TV-Z]{26}")) throw new GameError("请输入完整的存档码", "INVALID_INPUT"); return normalized;
    }
    /** 输入共享码，返回其唯一房间标识，缺失时给出可读错误。 */
    private String roomId(String archiveCode) {
        List<String> ids = jdbc.query("SELECT room_id FROM room_archives WHERE archive_code=?", (rs, row) -> rs.getString(1), code(archiveCode));
        if (ids.isEmpty()) throw new GameError("存档不存在，请核对存档码", "ARCHIVE_NOT_FOUND"); return ids.getFirst();
    }
    /** 输入保存行，返回格式或规则不兼容的说明，兼容返回空。 */
    private String compatibility(ResultSet rs) throws SQLException {
        if (rs.getInt("format_version") != ArchiveRules.FORMAT) return "保存格式与当前版本不兼容，可以查看历史";
        return rules.compatible(rs.getString("rules_digest")) ? null : "玩法规则已变化，可以查看历史；恢复需使用保存时的规则配置";
    }
    /** 输入共享码与可选保存点，读取完整内部状态，仅供恢复服务使用。 */
    public SavedRoom saved(String archiveCode, String snapshotId) {
        String id = roomId(archiveCode);
        List<SavedRoom> values = jdbc.query("SELECT * FROM room_snapshots WHERE room_id=?" + (snapshotId == null ? " ORDER BY version DESC LIMIT 1" : " AND snapshot_id=?"),
                (rs, row) -> new SavedRoom(id, rs.getString("snapshot_id"), json.readRoom(rs.getString("state")), compatibility(rs)), snapshotId == null ? new Object[]{id} : new Object[]{id, snapshotId});
        if (values.isEmpty()) throw new GameError("保存点不存在", "SNAPSHOT_NOT_FOUND"); return values.getFirst();
    }
    /** 输入保存行及是否预览，返回公开元数据与保存时局面。 */
    private ArchiveSnapshotVo snapshotView(ResultSet rs, boolean preview) throws SQLException {
        ArchiveSnapshotVo value = new ArchiveSnapshotVo(); value.snapshotId = rs.getString("snapshot_id"); value.gameId = rs.getString("game_id"); value.version = rs.getLong("version"); value.turnNumber = rs.getInt("turn_number");
        value.at = rs.getLong("at"); value.status = rs.getString("status"); value.label = rs.getString("label"); value.manual = rs.getBoolean("manual"); value.restoreError = compatibility(rs); value.restorable = value.restoreError == null;
        if (preview) value.state = json.read(rs.getString("public_state"), PublicRoomVo.class); return value;
    }
    /** 输入共享码和保存点，返回严格公开的历史局面，不重新计算资产。 */
    public ArchiveSnapshotVo snapshot(String archiveCode, String snapshotId) {
        String id = roomId(archiveCode);
        List<ArchiveSnapshotVo> values = jdbc.query("SELECT * FROM room_snapshots WHERE room_id=?" + (snapshotId == null ? " ORDER BY version DESC LIMIT 1" : " AND snapshot_id=?"),
                (rs, row) -> snapshotView(rs, true), snapshotId == null ? new Object[]{id} : new Object[]{id, snapshotId});
        if (values.isEmpty()) throw new GameError("保存点不存在", "SNAPSHOT_NOT_FOUND"); return values.getFirst();
    }
    /** 输入共享码与书签游标，固定次数批量读取索引，不逐对局查询。 */
    public ArchiveVo query(ArchiveInput input) {
        String id = roomId(input.archiveCode);
        ArchiveVo value = jdbc.queryForObject("SELECT a.*,s.archive_code AS source_archive_code FROM room_archives a LEFT JOIN room_archives s ON s.room_id=a.source_room_id WHERE a.room_id=?", (rs, row) -> {
            ArchiveVo result = new ArchiveVo(); result.roomId = id; result.archiveCode = rs.getString("archive_code"); result.code = rs.getString("code"); result.sourceArchiveCode = rs.getString("source_archive_code"); result.sourceSnapshotId = rs.getString("source_snapshot_id"); return result;
        }, id);
        value.latest = snapshot(input.archiveCode, null);
        value.games = jdbc.query("SELECT game_id,MIN(at) AS first_at,MAX(at) AS last_at,MAX(turn_number) AS turns,COUNT(*) AS snapshots FROM room_snapshots WHERE room_id=? GROUP BY game_id ORDER BY first_at DESC", (rs, row) -> {
            ArchiveGameVo game = new ArchiveGameVo(); game.gameId = rs.getString("game_id"); game.firstAt = rs.getLong("first_at"); game.lastAt = rs.getLong("last_at"); game.turnNumber = rs.getInt("turns"); game.snapshots = rs.getLong("snapshots"); return game;
        }, id);
        value.bookmarks = jdbc.query("SELECT * FROM room_snapshots WHERE room_id=? AND manual=TRUE AND version<? ORDER BY version DESC LIMIT 51", (rs, row) -> snapshotView(rs, false), id, input.cursor == null ? Long.MAX_VALUE : input.cursor);
        if (value.bookmarks.size() > 50) { value.bookmarks.removeLast(); value.nextCursor = value.bookmarks.getLast().version; } return value;
    }
    /** 输入历史筛选，绑定全部参数并返回倒序游标分页。 */
    public HistoryPageVo history(ArchiveInput input) {
        List<Object> params = new ArrayList<>(List.of(roomId(input.archiveCode), input.cursor == null ? Long.MAX_VALUE : input.cursor));
        String sql = "SELECT sequence,snapshot_id,game_id,version,turn_number,phase,actor_id,actor_name,type,text,at,details FROM room_history WHERE room_id=? AND sequence<?";
        if ("lobby".equals(input.gameId)) sql += " AND game_id IS NULL";
        else if (input.gameId != null) { sql += " AND game_id=?"; params.add(input.gameId); }
        if (input.turnNumber != null) { sql += " AND turn_number=?"; params.add(input.turnNumber); }
        if ("live".equals(input.type)) sql += " AND type IN ('log','card_used','legacy_log','chat')";
        else if (input.type != null) { sql += " AND type=?"; params.add(input.type); }
        HistoryPageVo page = new HistoryPageVo(); page.entries = jdbc.query(sql + " ORDER BY sequence DESC LIMIT 51", (rs, row) -> {
            HistoryEntryVo entry = new HistoryEntryVo(); entry.sequence = rs.getLong("sequence"); entry.snapshotId = rs.getString("snapshot_id"); entry.gameId = rs.getString("game_id"); entry.version = rs.getLong("version"); entry.turnNumber = (Integer) rs.getObject("turn_number"); entry.phase = rs.getString("phase"); entry.actorId = rs.getString("actor_id"); entry.actorName = rs.getString("actor_name"); entry.type = rs.getString("type"); entry.text = rs.getString("text"); entry.at = rs.getLong("at");
            String detail = rs.getString("details");
            if (detail != null && Set.of("log", "card_used", "chat").contains(entry.type)) {
                Map<?, ?> publicDetail = json.read(detail, Map.class); Object id = publicDetail.get("logId"); if (id instanceof String logId) entry.logId = logId;
                if (publicDetail.get("mentions") instanceof List<?> mentions) entry.mentions = List.of(json.read(json.write(mentions), PlayerMention[].class));
                if (publicDetail.get("references") instanceof List<?> references) entry.references = List.of(json.read(json.write(references), ChatReference[].class));
            }
            return entry;
        }, params.toArray());
        if (page.entries.size() > 50) { page.entries.removeLast(); page.nextCursor = page.entries.getLast().sequence; } return page;
    }
    /** 输入已版本校验房间，只更新书签元数据并关联一条公开保存事实。 */
    public ArchiveSnapshotVo mark(Room room, String label, String actorId) {
        if (jdbc.update("UPDATE room_snapshots SET manual=TRUE,label=? WHERE room_id=? AND version=?", label, room.roomId, room.version) != 1) throw new GameError("当前保存点不存在，请重新同步");
        Player actor = GameSupport.player(room, actorId);
        jdbc.update(HISTORY_INSERT, historyRow(room, new HistoryTrace.Entry(room.gameId, room.turnNumber, room.phase, actorId, actor == null ? null : actor.name, "save", "标记存档" + (label == null || label.isBlank() ? "" : "：" + label), System.currentTimeMillis(), null)));
        return snapshot(room.archiveCode, snapshotId(room));
    }
    /** 输入源房间与请求 ID，返回首次恢复结果及其请求摘要。 */
    public RestoredRoom restored(String sourceRoomId, String requestId) {
        List<RestoredRoom> values = jdbc.query("SELECT code,restored_player_id,restore_digest FROM room_archives WHERE source_room_id=? AND restore_request_id=?", (rs, row) -> new RestoredRoom(rs.getString(1), rs.getString(2), rs.getString(3)), sourceRoomId, requestId);
        return values.isEmpty() ? null : values.getFirst();
    }
    /** 输入新房间和首次请求，在当前事务记录去重索引。 */
    public void bindRestore(Room room, String requestId, String digest, String playerId) {
        jdbc.update("UPDATE room_archives SET restore_request_id=?,restore_digest=?,restored_player_id=? WHERE room_id=?", requestId, digest, playerId, room.roomId);
    }
}
