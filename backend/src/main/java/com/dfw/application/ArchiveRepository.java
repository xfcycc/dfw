package com.dfw.application;

import com.dfw.domain.Room;
import com.dfw.interfaces.dto.ArchiveInput;
import com.dfw.interfaces.vo.*;
import java.util.List;

/** 完整历史与不可变进度快照的持久化端口。 */
public interface ArchiveRepository {
    /** 服务端专用恢复资料，不作为 HTTP 响应。 */
    record SavedRoom(String roomId, String snapshotId, Room room, String restoreError) { }
    /** 同一个恢复请求已经创建的房间与首次请求摘要。 */
    record RestoredRoom(String code, String playerId, String digest) { }
    /** 输入房间，分配缺失的稳定标识及固定存档码，不访问数据库。 */
    void identify(Room room);
    /** 输入已有房间，批量补齐档案、会话归属和当前基线快照。 */
    void initialize(List<Room> rooms);
    /** 输入已提交候选，在调用者事务内保存档案、快照与本次事实。 */
    void append(Room room);
    /** 输入共享码与保存点，返回仅服务端可用的完整进度。 */
    SavedRoom saved(String archiveCode, String snapshotId);
    /** 输入码和分页游标，返回房间、对局与手动保存点索引。 */
    ArchiveVo query(ArchiveInput input);
    /** 输入码、筛选条件和游标，返回公开历史分页。 */
    HistoryPageVo history(ArchiveInput input);
    /** 输入码和保存点，返回公开历史局面与兼容性。 */
    ArchiveSnapshotVo snapshot(String archiveCode, String snapshotId);
    /** 输入当前房间及备注，只标记当前保存点并追加保存事实。 */
    ArchiveSnapshotVo mark(Room room, String label, String actorId);
    /** 输入源房间及请求标识，返回已有恢复结果或空。 */
    RestoredRoom restored(String sourceRoomId, String requestId);
    /** 输入新房间及请求资料，在同事务绑定恢复请求。 */
    void bindRestore(Room room, String requestId, String digest, String playerId);
}
