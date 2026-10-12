package com.dfw.infrastructure;

import org.springframework.stereotype.Component;
import com.dfw.domain.Room;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.DeserializationFeature;

/** 快照与协议 JSON 序列化，保留空值和旧快照字段。 */
@Component
public class JsonCodec {
    public final JsonMapper mapper = JsonMapper.builder().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();
    /** 输入对象，返回 JSON 文本。 */
    public String write(Object value) { return mapper.writeValueAsString(value); }
    /** 输入 JSON 与目标类，返回反序列化结果。 */
    public <T> T read(String json, Class<T> type) { return mapper.readValue(json, type); }
    /** 输入旧快照，兼容未保存 jailed 字段的玩家，返回内部房间。 */
    public Room readRoom(String snapshot) {
        Room room = read(snapshot, Room.class); var players = mapper.readTree(snapshot).get("players");
        for (int i = 0; i < room.players.size(); i++) {
            var player = room.players.get(i);
            if (!players.get(i).has("jailed")) player.jailed = player.skipTurns > 0 && player.position < room.board.size() && room.board.get(player.position).type.equals("jail");
        }
        return room;
    }
    /** 输入候选源与目标类型，返回独立副本。 */
    public <T> T copy(Object value, Class<T> type) { return read(write(value), type); }
}
