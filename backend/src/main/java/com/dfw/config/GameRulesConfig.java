package com.dfw.config;

import com.dfw.domain.rules.*;
import com.dfw.domain.EconomyRules;
import com.dfw.domain.MapRegions;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.DeserializationFeature;

/** 启动时严格加载四类规则文件，编译成唯一规则集，不保留版本目录。 */
@Configuration
public class GameRulesConfig {
    /** 输入四类文件地址，返回本进程共用的不可变规则集，不在对局中读取文件。 */
    @Bean
    public RuleSet gameRules(@Value("${game.rules.chance-events:classpath:game-rules/chance-events.json}") String chance,
            @Value("${game.rules.fortune-events:classpath:game-rules/fortune-events.json}") String fortune,
            @Value("${game.rules.card-events:classpath:game-rules/card-events.json}") String cards,
            @Value("${game.rules.turn-rules:classpath:game-rules/turn-rules.json}") String turn) throws IOException {
        JsonMapper mapper = JsonMapper.builder().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();
        var chanceFile = read(chance, RuleDefinition.ChanceFile.class, mapper);
        RuleDefinition definition = new RuleDefinition(); definition.chanceEvents = chanceFile.events; definition.passiveRules = chanceFile.passiveRules;
        var fortuneFile = read(fortune, RuleDefinition.FortuneFile.class, mapper);
        var cardFile = read(cards, RuleDefinition.CardFile.class, mapper);
        definition.fortuneEvents = fortuneFile.events; definition.cards = cardFile.cards;
        definition.content = new ContentCatalog(chanceFile.content, fortuneFile.content, cardFile.content);
        definition.stages = read(turn, RuleDefinition.TurnFile.class, mapper).stages;
        return RuleSet.compile(definition);
    }
    /** 输入独立经济文件地址，校验新局倍率及最终金额覆盖。 */
    @Bean
    public EconomyRules gameEconomy(@Value("${game.rules.economy:classpath:game-rules/economy.json}") String location) throws IOException {
        EconomyRules value = read(location, EconomyRules.class, JsonMapper.builder().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build());
        value.validate(); return value;
    }
    /** 输入地区文件地址，加载只参与地图命名的地区组。 */
    @Bean
    public MapRegions mapRegions(@Value("${game.rules.map-regions:classpath:game-rules/map-regions.json}") String location) throws IOException {
        MapRegions value = read(location, MapRegions.class, JsonMapper.builder().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build());
        value.validate(); return value;
    }
    /** 输入单文件地址、独立根类型及严格 JSON 解析器，限制文件大小后返回定义。 */
    private static <T> T read(String location, Class<T> type, JsonMapper mapper) throws IOException {
        var resource = new DefaultResourceLoader().getResource(location); byte[] bytes;
        try (var input = resource.getInputStream()) { bytes = input.readNBytes(262145); }
        if (bytes.length > 262144) throw new IllegalArgumentException("规则文件超过 256 KiB：" + location);
        T value = mapper.readValue(bytes, type);
        if (value == null) throw new IllegalArgumentException("规则文件不能为空：" + location);
        return value;
    }
}
