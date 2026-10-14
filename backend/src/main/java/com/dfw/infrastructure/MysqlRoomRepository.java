package com.dfw.infrastructure;

import com.dfw.application.RoomRepository;
import com.dfw.application.ArchiveRepository;
import com.dfw.domain.Room;
import com.dfw.interfaces.vo.CommandResult;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

/** MySQL InnoDB 快照与命令回执存储，保持原 JSON 契约。 */
@Repository
public class MysqlRoomRepository implements RoomRepository {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;
    private final JsonCodec json;
    private final ArchiveRepository archives;
    /** 输入 JDBC、事务和 JSON 编解码器，使用启动阶段初始化的表结构。 */
    public MysqlRoomRepository(JdbcTemplate jdbc, TransactionTemplate transaction, JsonCodec json, ArchiveRepository archives) {
        this.jdbc = jdbc; this.transaction = transaction; this.json = json;
        this.archives = archives;

    }
    /** 一次查询返回全部房间，不逐玩家查询。 */
    public List<Room> loadRooms() { return jdbc.query("SELECT state FROM rooms", (rs, row) -> json.readRoom(rs.getString(1))); }
    /** 输入令牌摘要，查找会话。 */
    public StoredSession session(String hash) {
        List<StoredSession> values = jdbc.query("SELECT code, player_id,room_id FROM sessions WHERE token_hash = ?", (rs, row) -> new StoredSession(hash, rs.getString(1), rs.getString(2), rs.getString(3)), hash);
        return values.isEmpty() ? null : values.getFirst();
    }
    /** 输入房间和命令标识，查找原提交结果。 */
    public Receipt receipt(String code, String id) {
        List<Receipt> values = jdbc.query("SELECT actor_id, result FROM receipts WHERE code = ? AND command_id = ?", (rs, row) -> {
            String stored = rs.getString(2); var digest = json.mapper.readTree(stored).get("requestDigest");
            return new Receipt(id, rs.getString(1), json.read(stored, CommandResult.class), digest == null || digest.isNull() ? null : digest.asString());
        }, code, id);
        return values.isEmpty() ? null : values.getFirst();
    }
    /** 输入房间及可选身份和回执，原子提交或整体回滚。 */
    public void save(Room room, StoredSession session, Receipt receipt) {
        transaction.executeWithoutResult(status -> {
            archives.append(room);
            String state = json.write(room);
            jdbc.update("INSERT INTO rooms(code,room_id,state) VALUES (?,?,?) ON DUPLICATE KEY UPDATE state=?,room_id=?", room.code, room.roomId, state, state, room.roomId);
            if (session != null) jdbc.update("INSERT INTO sessions(token_hash,code,player_id,room_id) VALUES (?,?,?,?)", session.tokenHash(), session.code(), session.playerId(), session.roomId());
            if (receipt != null) {
                var stored = json.mapper.valueToTree(receipt.result()).deepCopy();
                ((tools.jackson.databind.node.ObjectNode) stored).put("requestDigest", receipt.requestDigest());
                jdbc.update("INSERT INTO receipts(code,command_id,actor_id,result) VALUES (?,?,?,?)", room.code, receipt.id(), receipt.actorId(), json.write(stored));
            }
        });
    }
    /** 输入观战身份，独立写入会话。 */
    public void addSpectator(StoredSession session) { jdbc.update("INSERT INTO sessions(token_hash,code,player_id,room_id) VALUES (?,?,?,?)", session.tokenHash(), session.code(), session.playerId(), session.roomId()); }
    /** 输入访客和保存角色，只修改此房间此角色的会话，不触及资产或其他房间。 */
    public void assignSeat(Room room, String tokenHash, String playerId) {
        jdbc.update("DELETE FROM sessions WHERE room_id=? AND player_id=? AND token_hash<>?", room.roomId, playerId, tokenHash);
        if (jdbc.update("UPDATE sessions SET player_id=? WHERE token_hash=? AND room_id=?", playerId, tokenHash, room.roomId) != 1) throw new com.dfw.domain.GameError("申请身份已失效，请重新申请");
    }
}
