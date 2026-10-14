package com.dfw.application;

import com.dfw.domain.GameError;
import com.dfw.interfaces.dto.ArchiveInput;
import com.dfw.interfaces.dto.SeatClaimInput;
import com.dfw.interfaces.vo.*;
import org.springframework.stereotype.Service;

/** 共享存档码读写入口；恢复与身份操作复用房间应用服务的锁和事务。 */
@Service
public class ArchiveService {
    private final ArchiveRepository archives;
    private final RoomService rooms;
    /** 输入档案端口和房间服务，组装免账号存档能力。 */
    public ArchiveService(ArchiveRepository archives, RoomService rooms) { this.archives = archives; this.rooms = rooms; }
    /** 输入编码和游标，返回房间档案索引。 */
    public ArchiveVo query(ArchiveInput input) { validate(input); return archives.query(input); }
    /** 输入筛选，返回公开历史分页。 */
    public HistoryPageVo history(ArchiveInput input) { validate(input); return archives.history(input); }
    /** 输入编码和可选保存点，返回只读公开局面。 */
    public ArchiveSnapshotVo snapshot(ArchiveInput input) { validate(input); return archives.snapshot(input.archiveCode, input.snapshotId); }
    /** 输入会话、版本和备注，标记已有保存点，返回保存信息。 */
    public ArchiveSnapshotVo save(ArchiveInput input) { validate(input); return rooms.saveArchive(input); }
    /** 输入共享码、请求 ID 与选定角色，返回新恢复房间的会话。 */
    public SessionVo restore(ArchiveInput input) { validate(input); uuid(input.requestId, true); return rooms.restoreArchive(input); }
    /** 输入访客身份和保存角色，返回已持久化的申请。 */
    public SeatClaimVo claim(SeatClaimInput input) { if (input == null) throw new GameError("请求参数不正确", "INVALID_INPUT"); uuid(input.playerId, true); return rooms.claim(input); }
    /** 输入通用查询，校验标识与分页边界，不对可选备注增加业务限制。 */
    private void validate(ArchiveInput input) {
        if (input == null || input.cursor != null && input.cursor < 0 || input.turnNumber != null && input.turnNumber < 0) throw new GameError("请求参数不正确", "INVALID_INPUT");
        uuid(input.snapshotId, false); if (!"lobby".equals(input.gameId)) uuid(input.gameId, false); uuid(input.playerId, false);
    }
    /** 输入标识与是否必传，确认完整 UUID，拒绝无效查询或去重标识。 */
    private void uuid(String value, boolean required) {
        if (value == null && !required) return;
        if (value == null || !value.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-8][0-9a-fA-F]{3}-[89aAbB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}")) throw new GameError("请求参数不正确", "INVALID_INPUT");
    }
}
