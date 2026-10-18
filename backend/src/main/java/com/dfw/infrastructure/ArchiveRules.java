package com.dfw.infrastructure;

import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.stereotype.Component;

/** 只用于判断历史快照可否继续，不改变现有进行中对局的规则更新方式。 */
@Component
public class ArchiveRules {
    public static final int FORMAT = 1;
    /** e95cc4c 默认四类规则摘要，路障仅允许放在地产。 */
    private static final String PREVIOUS_BARRIER_RULES = "46709d3ca47ef8ad32dca845b91bcb56f929fae9eb15f0e9d9b6c2d5067b7ed6";
    /** 当前默认四类规则摘要，路障允许放在前方六格内任意无路障地块。 */
    private static final String CURRENT_BARRIER_RULES = "9bec850b41eecf601a0f20cac3448e6f2425185c034eb6f8989cb3226454c247";
    /** 本次扩展前默认规则，兼容缺省新增状态的已保存对局。 */
    private static final String PRE_EXPANSION_RULES = "b386ed8437b2b1ead3c5b4ece63fe93e4abd83e425b8778ccc9d4b7aff4ebbe4";
    /** 已确认十九项扩展内容的默认规则摘要，不放宽任意外部配置。 */
    private static final String EXPANSION_RULES = "13733a2ff165c91a601ae06128896ed1f6eb89523eb1a30a5858257f8e5b7e3d";
    public final String digest;
    /** 输入四类规则配置，规范化 JSON 对象键后计算一次兼容性摘要。 */
    public ArchiveRules(JsonCodec json,
            @Value("${game.rules.chance-events:classpath:game-rules/chance-events.json}") String chance,
            @Value("${game.rules.fortune-events:classpath:game-rules/fortune-events.json}") String fortune,
            @Value("${game.rules.card-events:classpath:game-rules/card-events.json}") String cards,
            @Value("${game.rules.turn-rules:classpath:game-rules/turn-rules.json}") String turn) throws Exception {
        List<Object> values = new ArrayList<>();
        for (String location : List.of(chance, fortune, cards, turn)) {
            try (var input = new DefaultResourceLoader().getResource(location).getInputStream()) {
                values.add(normalize(json.mapper.readValue(input, Object.class)));
            }
        }
        digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json.write(values).getBytes(StandardCharsets.UTF_8)));
    }
    /** 输入保存时的规则摘要，返回能否保留原进度并按当前规则继续；仅额外兼容明确的旧默认规则与本次新增字段缺省。 */
    public boolean compatible(String savedDigest) {
        return digest.equals(savedDigest) || EXPANSION_RULES.equals(digest) && PRE_EXPANSION_RULES.equals(savedDigest) || CURRENT_BARRIER_RULES.equals(digest) && PREVIOUS_BARRIER_RULES.equals(savedDigest);
    }
    /** 输入 JSON 值，递归排序对象键，保留数组顺序并返回规范化结构。 */
    private static Object normalize(Object value) {
        if (value instanceof Map<?, ?> map) { Map<String, Object> sorted = new TreeMap<>(); map.forEach((key, item) -> sorted.put(key.toString(), normalize(item))); return sorted; }
        if (value instanceof List<?> list) return list.stream().map(ArchiveRules::normalize).toList();
        return value;
    }
}
