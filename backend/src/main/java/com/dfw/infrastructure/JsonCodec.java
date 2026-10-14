package com.dfw.infrastructure;

import org.springframework.stereotype.Component;
import com.dfw.domain.Room;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.DeserializationFeature;
import com.dfw.domain.Action;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.TreeMap;

/** 快照与协议 JSON 序列化，保留空值。 */
@Component
public class JsonCodec {
    public final JsonMapper mapper = JsonMapper.builder().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();
    /** 输入对象，返回 JSON 文本。 */
    public String write(Object value) { return mapper.writeValueAsString(value); }
    /** 输入 JSON 与目标类，返回反序列化结果。 */
    public <T> T read(String json, Class<T> type) { return mapper.readValue(json, type); }
    /** 输入当前格式快照，返回内部房间，不迁移旧存档。 */
    public Room readRoom(String snapshot) { return read(snapshot, Room.class); }
    /** 输入候选源与目标类型，返回独立副本。 */
    public <T> T copy(Object value, Class<T> type) { return read(write(value), type); }
    /** 输入已按协议清理的领域操作，返回按字段名排序的 SHA-256；网络关联及预期版本不参与业务去重。 */
    public String actionDigest(Action action) {
        var fields = mapper.convertValue(action, TreeMap.class);
        // 新增可选金额省略或为零时，保持既有回执的规范化摘要，兼容重放。
        if (action.debtCredit == null || action.debtCredit == 0) fields.remove("debtCredit");
        if (action.mapSize == null) fields.remove("mapSize");
        if (action.decisionId == null) fields.remove("decisionId");
        if (action.keepCardIds == null) fields.remove("keepCardIds");
        String canonical = write(fields);
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException error) { throw new IllegalStateException(error); }
    }
}
