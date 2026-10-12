package com.dfw.interfaces;

import com.dfw.application.RoomService;
import com.dfw.domain.*;
import com.dfw.infrastructure.*;
import com.dfw.interfaces.dto.GameCommand;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.JsonNode;

/** 原生 WebSocket 协议适配器，游戏处理统一委托应用层。 */
@Component
public class GameWebSocketHandler extends TextWebSocketHandler {
    private final RoomService rooms;
    private final WebSocketPublisher publisher;
    private final JsonCodec json;
    /** 输入应用服务、投递适配器和编解码器。 */
    public GameWebSocketHandler(RoomService rooms, WebSocketPublisher publisher, JsonCodec json) { this.rooms = rooms; this.publisher = publisher; this.json = json; }
    /** 输入新连接，注册认证超时和文本消息上限。 */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) { session.setTextMessageSizeLimit(65536); publisher.open(session); }
    /** 输入文本信封，验证认证、同步或命令，并关联命令回执。 */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String id = session.getId(), requestId = null, type = null;
        try {
            JsonNode envelope;
            try { envelope = json.mapper.readTree(message.getPayload()); } catch (RuntimeException error) { throw new GameError("请求参数不正确", "INVALID_INPUT"); }
            if (!envelope.isObject() || !envelope.has("type") || !envelope.get("type").isString()) throw new GameError("请求参数不正确", "INVALID_INPUT");
            type = envelope.get("type").asString(); requestId = envelope.has("requestId") && envelope.get("requestId").isString() ? envelope.get("requestId").asString() : null;
            publisher.touch(id); JsonNode payload = envelope.get("payload");
            if (type.equals("session:authenticate")) {
                if (publisher.session(id) != null) throw new GameError("连接已认证", "INVALID_ACTION");
                String token = payload != null && payload.has("token") && payload.get("token").isString() ? payload.get("token").asString() : null;
                rooms.authenticate(id, token); return;
            }
            if (publisher.session(id) == null) throw new GameError("请先认证连接", "FORBIDDEN");
            switch (type) {
                case "room:sync" -> rooms.sync(id);
                case "heartbeat" -> publisher.send(id, "heartbeat:ack", requestId, null);
                case "game:command" -> {
                    GameCommand command = InputValidation.command(payload, json);
                    publisher.send(id, "command:result", requestId, rooms.command(id, command.id, command.expectedVersion, json.copy(command.action, Action.class)));
                }
                default -> throw new GameError("当前消息不可用", "INVALID_INPUT");
            }
        } catch (Exception error) {
            publisher.send(id, "game:command".equals(type) ? "command:result" : "room:error", requestId, Failures.result(error));
            if (publisher.session(id) == null) publisher.close(id);
        }
    }
    /** 输入已关闭连接，提交离线状态并清理连接注册。 */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) { rooms.disconnected(session.getId()); }
    /** 每秒清理认证超时和已失去心跳的连接。 */
    @Scheduled(fixedDelay = 1000)
    public void expireConnections() { publisher.expire(System.currentTimeMillis()); }
}
