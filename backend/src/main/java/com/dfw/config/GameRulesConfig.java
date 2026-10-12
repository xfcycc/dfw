package com.dfw.config;

import com.dfw.domain.rules.*;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.DeserializationFeature;

/** 从版本文件加载并编译规则，启动校验失败时不开放游戏服务。 */
@Configuration
public class GameRulesConfig {
    /** 输入新局版本及版本文件列表，返回不热更新的规则目录。 */
    @Bean
    public RuleCatalog ruleCatalog(@Value("${game.rules.active-version:classic-v2}") String activeVersion,
            @Value("${game.rules.resources:classpath:game-rules/classic-v2.json}") String locations) throws IOException, NoSuchAlgorithmException {
        JsonMapper mapper = JsonMapper.builder().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build(); Map<String, RuleSet> versions = new LinkedHashMap<>();
        var resolver = new PathMatchingResourcePatternResolver();
        for (String location : locations.split(",")) {
            Resource[] resources = resolver.getResources(location.strip());
            if (resources.length == 0) throw new IllegalArgumentException("规则文件不存在：" + location);
            for (Resource resource : resources) {
                byte[] bytes;
                try (var input = resource.getInputStream()) { bytes = input.readNBytes(262145); }
                if (bytes.length > 262144) throw new IllegalArgumentException("规则文件超过 256 KiB：" + resource.getDescription());
                String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
                RuleSet rules = RuleSet.compile(mapper.readValue(bytes, RuleDefinition.class), digest);
                if ("classic-v1".equals(rules.getVersion()) || versions.putIfAbsent(rules.getVersion(), rules) != null) throw new IllegalArgumentException("规则版本保留或重复：" + rules.getVersion());
            }
        }
        return new RuleCatalog(activeVersion, versions);
    }
}
