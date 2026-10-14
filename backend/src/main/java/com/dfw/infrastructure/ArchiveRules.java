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
    /** 输入 JSON 值，递归排序对象键，保留数组顺序并返回规范化结构。 */
    private static Object normalize(Object value) {
        if (value instanceof Map<?, ?> map) { Map<String, Object> sorted = new TreeMap<>(); map.forEach((key, item) -> sorted.put(key.toString(), normalize(item))); return sorted; }
        if (value instanceof List<?> list) return list.stream().map(ArchiveRules::normalize).toList();
        return value;
    }
}
