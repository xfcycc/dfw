package com.dfw.application;

import com.dfw.application.RoomRepository.StoredSession;
import com.dfw.domain.Room;

/** 联机投递端口，应用流程不依赖 Spring WebSocket API。 */
public interface StatePublisher {
    /** 输入连接和会话，替换同身份旧连接并加入房间。 */
    void attach(String connectionId, StoredSession session);
    /** 输入连接，返回其已认证会话或空。 */
    StoredSession session(String connectionId);
    /** 输入连接和会话，返回是否为该玩家的有效操作连接。 */
    boolean active(String connectionId, StoredSession session);
    /** 输入房间与玩家，返回其有效连接是否仍打开。 */
    boolean online(String code, String playerId);
    /** 输入令牌摘要，返回该访客是否有有效在线连接。 */
    boolean connected(String tokenHash);
    /** 输入连接与房间，同步其最新角色身份，不发送原始令牌。 */
    void identity(String connectionId, Room room);
    /** 输入提交后的角色分配，撤销旧角色连接并升级申请人的连接。 */
    void assign(Room room, StoredSession session);
    /** 输入已提交房间，广播公共状态及逐人私人状态。 */
    void broadcast(Room room);
    /** 输入连接和房间，发送该角色完整状态。 */
    void sync(String connectionId, Room room);
    /** 输入连接，移除其注册资料。 */
    void remove(String connectionId);
    /** 关闭所有连接，供优雅停机使用。 */
    void closeAll();
}
