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
    /** 仅精简四十六个卡名后的默认规则摘要，效果、参数和权重保持不变。 */
    private static final String RENAMED_CONTENT_RULES = "0265c821a96c52a3d1e93bbc6f9e26d659cc0087f62a99c2367265a87059c460";
    /** 本次十六张皮肤加入前的默认规则摘要，数值与权重沿用当前本地配置。 */
    private static final String PRE_SKIN_EXPANSION_RULES = "ba0e74d939ec42a8b2fe7942d09fa5b092e3a1ecdc20f164af03fe30a32e3e92";
    /** 仅追加十六张皮肤后的默认规则摘要，兼容追加前保存的同规则对局。 */
    private static final String SKIN_EXPANSION_RULES = "cc670f6c525d1a641ec6874fe5f44ff29d3bff67096b486f817bb43a21cc432f";
    /** 动漫与建设内容加入后的默认规则摘要，仅兼容明确登记的旧默认内容。 */
    private static final String ANIME_CONTENT_RULES = "3a8202718e597f290f2808f043fc12b59ed13f88e4acb3171559b38cae902d2f";
    /** 超电磁炮台词及描述改写前的同机制默认摘要，仅兼容明确的文案更新。 */
    private static final String PRE_RAILGUN_COPY_RULES = "33a62555361354f8339602e1a69fa52d45b8b8949f62bc8045ec8b54537684fa";
    /** 全卡文案审阅前的同机制默认摘要，允许保留原卡面继续对局。 */
    private static final String PRE_CARD_COPY_REVIEW_RULES = "3a8e4afa217ab084d1e759e97997aab29e3388862712d9969dc0f2796f1a2745";
    /** 三重罗生门独立为三层 UR 后的默认规则，旧卡保留原参数与等级。 */
    private static final String TRIPLE_SHIELD_RULES = "4043f8c7fb68f7ed5dc7e83506236eafd86674765e5a412bbce04202379bd580";
    /** 投影魔术升级为 UR 并可复制 UR，旧手牌保留冻结卡面及参数。 */
    private static final String UR_PROJECTION_RULES = "c03ab21ae5cdfb4b2f0e7745cc75dcaa7411f0e7dc9f7d161fdf27a00c49c230";
    /** 全部道具UR完整效果均分5%额度后的默认摘要，旧卡面和进度继续保留。 */
    private static final String EQUAL_UR_RULES = "3d9daa6260811a5ca0615b87f87faa4600808c39963486ba10bee14d725e33f2";
    /** 道具四档内完整效果均分后的默认摘要，保留原档位总出率及旧手牌。 */
    private static final String EQUAL_CARD_TIER_RULES = "341e825c8954c3be3a0dcc3d505970a58a5cf9e0a4f3afbbd5a8c3d589a6208e";
    /** 两回合连控保护与卡牌数值调整后的默认摘要，旧手牌金额与已排队分段保留。 */
    private static final String BALANCED_CARD_RULES = "5f92a3effbd0723504ba09a4fad93a9b04c4755ae60ec3f9060c26fa0ec00413";
    /** 八素材加入后的明确默认摘要，旧手牌与待结算结果保持冻结。 */
    private static final String NETA_CONTENT_RULES = "3f0d24724666412a879ef2e634787440b0a0ff6d1e5e605a1a11d5feff708e34";
    /** 十七素材扩展与精确长整型事件权重的默认摘要，仅兼容明确登记的历史默认内容。 */
    private static final String CROSSOVER_CONTENT_RULES = "3f0d24724666412a879ef2e634787440b0a0ff6d1e5e605a1a11d5feff708e34";
    /** 八素材最终UR设计的明确默认摘要，旧手牌及旧选择仍按冻结版本结算。 */
    private static final String FINAL_NETA_RULES = "6d9d0a5808502360c937636a7111f5a1e0ec258797b394df882e1b9f25103c7b";
    /** 王的压制改为SSR三回合主动攻击限制后的默认摘要，历史卡牌与状态继续冻结。 */
    private static final String ATTACK_PRESSURE_RULES = "d28a0b9099f7982a43f498b5e0c27bad8c107f736eea9f1b62d846febc62e709";
    /** 小舞献祭加入后的默认摘要，保留旧卡和历史快照。 */
    private static final String XIAOWU_RULES = "cf1033a89675725649ae8f33aebe13c8d6570d63089fe7877c6bfe4c816fbc34";
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
    /** 输入保存时的规则摘要，返回能否保留原进度继续；仅额外兼容明确登记的旧默认规则与扩展字段缺省，不放宽外部规则配置。 */
    public boolean compatible(String savedDigest) {
        return digest.equals(savedDigest)
                || (FINAL_NETA_RULES.equals(digest) || ATTACK_PRESSURE_RULES.equals(digest) || XIAOWU_RULES.equals(digest)) && savedDigest!=null && Set.of(ATTACK_PRESSURE_RULES,FINAL_NETA_RULES,PREVIOUS_BARRIER_RULES,CURRENT_BARRIER_RULES,PRE_EXPANSION_RULES,EXPANSION_RULES,RENAMED_CONTENT_RULES,PRE_SKIN_EXPANSION_RULES,SKIN_EXPANSION_RULES,ANIME_CONTENT_RULES,PRE_RAILGUN_COPY_RULES,PRE_CARD_COPY_REVIEW_RULES,TRIPLE_SHIELD_RULES,UR_PROJECTION_RULES,EQUAL_UR_RULES,EQUAL_CARD_TIER_RULES,BALANCED_CARD_RULES,NETA_CONTENT_RULES,"aeda9c8378265ecb71cc6d53d985bd90e19dc28b418be61c0c2d44fb38365e46").contains(savedDigest)
                || CROSSOVER_CONTENT_RULES.equals(digest) && savedDigest!=null && Set.of(PREVIOUS_BARRIER_RULES,CURRENT_BARRIER_RULES,PRE_EXPANSION_RULES,EXPANSION_RULES,RENAMED_CONTENT_RULES,PRE_SKIN_EXPANSION_RULES,SKIN_EXPANSION_RULES,ANIME_CONTENT_RULES,PRE_RAILGUN_COPY_RULES,PRE_CARD_COPY_REVIEW_RULES,TRIPLE_SHIELD_RULES,UR_PROJECTION_RULES,EQUAL_UR_RULES,EQUAL_CARD_TIER_RULES,BALANCED_CARD_RULES,NETA_CONTENT_RULES).contains(savedDigest)
                || NETA_CONTENT_RULES.equals(digest) && savedDigest!=null && Set.of(PREVIOUS_BARRIER_RULES,CURRENT_BARRIER_RULES,PRE_EXPANSION_RULES,EXPANSION_RULES,RENAMED_CONTENT_RULES,PRE_SKIN_EXPANSION_RULES,SKIN_EXPANSION_RULES,ANIME_CONTENT_RULES,PRE_RAILGUN_COPY_RULES,PRE_CARD_COPY_REVIEW_RULES,TRIPLE_SHIELD_RULES,UR_PROJECTION_RULES,EQUAL_UR_RULES,EQUAL_CARD_TIER_RULES,BALANCED_CARD_RULES).contains(savedDigest)
                || (EQUAL_CARD_TIER_RULES.equals(digest) || BALANCED_CARD_RULES.equals(digest)) && (EQUAL_CARD_TIER_RULES.equals(savedDigest) || EQUAL_UR_RULES.equals(savedDigest) || UR_PROJECTION_RULES.equals(savedDigest) || TRIPLE_SHIELD_RULES.equals(savedDigest))
                || EQUAL_UR_RULES.equals(digest) && (UR_PROJECTION_RULES.equals(savedDigest) || TRIPLE_SHIELD_RULES.equals(savedDigest))
                || UR_PROJECTION_RULES.equals(digest) && TRIPLE_SHIELD_RULES.equals(savedDigest)
                || (ANIME_CONTENT_RULES.equals(digest) || TRIPLE_SHIELD_RULES.equals(digest) || UR_PROJECTION_RULES.equals(digest) || EQUAL_UR_RULES.equals(digest) || EQUAL_CARD_TIER_RULES.equals(digest) || BALANCED_CARD_RULES.equals(digest)) && savedDigest != null && Set.of(ANIME_CONTENT_RULES, PRE_CARD_COPY_REVIEW_RULES, PRE_RAILGUN_COPY_RULES, SKIN_EXPANSION_RULES, PRE_SKIN_EXPANSION_RULES, RENAMED_CONTENT_RULES, EXPANSION_RULES, PRE_EXPANSION_RULES).contains(savedDigest)
                || SKIN_EXPANSION_RULES.equals(digest) && PRE_SKIN_EXPANSION_RULES.equals(savedDigest)
                || RENAMED_CONTENT_RULES.equals(digest) && (EXPANSION_RULES.equals(savedDigest) || PRE_EXPANSION_RULES.equals(savedDigest))
                || EXPANSION_RULES.equals(digest) && PRE_EXPANSION_RULES.equals(savedDigest) || CURRENT_BARRIER_RULES.equals(digest) && PREVIOUS_BARRIER_RULES.equals(savedDigest);
    }
    /** 输入 JSON 值，递归排序对象键，保留数组顺序并返回规范化结构。 */
    private static Object normalize(Object value) {
        if (value instanceof Map<?, ?> map) { Map<String, Object> sorted = new TreeMap<>(); map.forEach((key, item) -> sorted.put(key.toString(), normalize(item))); return sorted; }
        if (value instanceof List<?> list) return list.stream().map(ArchiveRules::normalize).toList();
        return value;
    }
}
