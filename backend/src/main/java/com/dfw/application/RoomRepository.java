package com.dfw.application;

import com.dfw.domain.Room;
import com.dfw.interfaces.vo.CommandResult;
import java.util.List;

/** 房间持久化端口，应用层不依赖具体数据库。 */
public interface RoomRepository {
    /** 令牌摘要对应的身份。 */
    record StoredSession(String tokenHash, String code, String playerId) { }
    /** 已事务提交的命令回执。 */
    record Receipt(String id, String actorId, CommandResult result) { }
    /** 批量返回全部可恢复房间。 */
    List<Room> loadRooms();
    /** 输入令牌摘要，返回身份或空。 */
    StoredSession session(String hash);
    /** 输入房间号和命令标识，返回历史结果或空。 */
    Receipt receipt(String code, String id);
    /** 输入候选房间及可选身份、命令回执，一次事务保存。 */
    void save(Room room, StoredSession session, Receipt receipt);
    /** 输入观战身份，独立保存，不改变房间版本。 */
    void addSpectator(StoredSession session);
}
