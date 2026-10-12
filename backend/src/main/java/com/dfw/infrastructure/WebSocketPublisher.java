package com.dfw.infrastructure;

import com.dfw.application.StatePublisher;
import com.dfw.application.RoomRepository.StoredSession;
import com.dfw.domain.Room;
import com.dfw.interfaces.StateViews;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

/** 连接注册和白名单状态投递，所有底层发送都保护并发写入。 */
@Component
public class WebSocketPublisher implements StatePublisher {
    private final JsonCodec json;
    private final Map<String, WebSocketSession> connections = new ConcurrentHashMap<>();
    private final Map<String, StoredSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, String> active = new ConcurrentHashMap<>();
    private final Map<String, Long> lastSeen = new ConcurrentHashMap<>();
    private final Map<String, Long> opened = new ConcurrentHashMap<>();
    /** 输入 JSON 编解码器，构造联机投递适配器。 */
    public WebSocketPublisher(JsonCodec json) { this.json = json; }
    /** 输入新连接，注册并记录认证期限。 */
    public void open(WebSocketSession session) {
        connections.put(session.getId(), new ConcurrentWebSocketSessionDecorator(session, 5000, 262144));
        opened.put(session.getId(), System.currentTimeMillis()); lastSeen.put(session.getId(), System.currentTimeMillis());
    }
    /** 输入收到消息的连接，更新时间戳。 */
    public void touch(String id) { lastSeen.put(id, System.currentTimeMillis()); }
    /** 输入已认证连接与身份，替换旧连接并回应认证成功。 */
    public void attach(String id, StoredSession session) {
        if (!connections.containsKey(id) || !connections.get(id).isOpen()) throw new IllegalStateException("连接已关闭");
        sessions.put(id, session);
        if (session.playerId() != null) {
            String previous = active.put(key(session), id);
            if (previous != null && !previous.equals(id)) { send(previous, "session:replaced", null, null); close(previous); }
        }
        send(id, "session:authenticated", null, Map.of("ok", true));
    }
    /** 输入连接，返回认证资料或空。 */
    public StoredSession session(String id) { return sessions.get(id); }
    /** 输入连接和身份，确认仍是可操作连接。 */
    public boolean active(String id, StoredSession session) { return id.equals(active.get(key(session))); }
    /** 输入房间与玩家，返回有效底层连接是否仍打开。 */
    public boolean online(String code, String playerId) {
        String id = active.get(code + ":" + playerId); WebSocketSession connection = id == null ? null : connections.get(id);
        return connection != null && connection.isOpen();
    }
    /** 输入会话，返回玩家连接索引。 */
    private String key(StoredSession session) { return session.code() + ":" + session.playerId(); }
    /** 输入房间，发送公开快照，私人状态只发到本人有效连接。 */
    public void broadcast(Room room) {
        Object publicState = StateViews.publicRoom(room);
        sessions.forEach((id, session) -> { if (session.code().equals(room.code)) send(id, "room:snapshot", null, publicState); });
        for (var player : room.players) {
            String id = active.get(room.code + ":" + player.id);
            if (id != null) send(id, "player:private", null, StateViews.privatePlayer(room, player.id));
        }
    }
    /** 输入连接和房间，按认证角色发送完整状态。 */
    public void sync(String id, Room room) {
        StoredSession session = sessions.get(id); if (session == null) return;
        send(id, "room:snapshot", null, StateViews.publicRoom(room));
        if (session.playerId() != null && active(id, session)) send(id, "player:private", null, StateViews.privatePlayer(room, session.playerId()));
    }
    /** 输入连接、消息类型、请求标识和白名单载荷，串行安全发送。 */
    public void send(String id, String type, String requestId, Object payload) {
        WebSocketSession connection = connections.get(id); if (connection == null || !connection.isOpen()) return;
        Map<String, Object> message = new LinkedHashMap<>(); message.put("type", type); if (requestId != null) message.put("requestId", requestId); message.put("payload", payload);
        try { connection.sendMessage(new TextMessage(json.write(message))); }
        catch (IOException | RuntimeException error) { close(id); }
    }
    /** 输入连接，移除注册信息，仅删除仍匹配的玩家索引。 */
    public void remove(String id) { StoredSession session = sessions.remove(id); if (session != null) active.remove(key(session), id); connections.remove(id); lastSeen.remove(id); opened.remove(id); }
    /** 输入连接，关闭底层连接，离线状态由 Handler 回调提交。 */
    public void close(String id) {
        WebSocketSession connection = connections.get(id);
        try { if (connection != null && connection.isOpen()) connection.close(); } catch (IOException ignored) { }
    }
    /** 输入当前时间，关闭认证超时或六十秒无消息的连接。 */
    public void expire(long now) {
        for (String id : connections.keySet()) if ((!sessions.containsKey(id) && now - opened.getOrDefault(id, now) >= 5000) || now - lastSeen.getOrDefault(id, now) >= 60000) close(id);
    }
    /** 关闭全部连接。 */
    public void closeAll() { for (String id : connections.keySet()) close(id); }
}
