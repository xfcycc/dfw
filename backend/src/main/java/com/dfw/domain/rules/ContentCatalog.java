package com.dfw.domain.rules;

import com.dfw.domain.*;
import java.util.*;
import java.util.function.ToIntFunction;
import static com.dfw.domain.GameError.require;

/** 启动校验内容层，先抽影响等级，再等概率抽完整效果及匹配卡面，结果进入房间快照。 */
public final class ContentCatalog {
    private static final List<String> RARITIES = List.of("R","SR","SSR","UR");
    private static final Map<String, Integer> RARITY = Map.of("R",40,"SR",30,"SSR",25,"UR",5);
    private static final Set<String> CASH = Set.of("amount","wager","prize","bonus","fallback");
    private static final Set<String> CARD = Set.of("fixed","remote","barrier","demolish","swap","immunity","move","bonus","freeUpgrade","halfPayment","release","steal","cash","gamble");
    private static final Set<String> EVENT = Set.of("income","fee","forward","back","skip","cards","cards_one","cards_two","directed","all_income","all_fee","rich_fee","bottom_income","gamble","self_destroy","disaster","free_upgrade","trap_immunity","next_waiver","rent_double","bottom_bonus","extra_action");
    private final Map<String, ContentDefinition> pools;
    /** 一个去重后的机制与完整参数组合，名称和插画不增加其概率。 */
    private record Candidate(String mechanism, ContentDefinition.Parameter parameter) {}
    /** 输入三类文件内容，启动时核对概率、去重参数及皮肤身份，返回唯一内容目录。 */
    public ContentCatalog(ContentDefinition chance, ContentDefinition fortune, ContentDefinition cards) {
        require(chance!=null && fortune!=null && cards!=null,"三份事件文件均须提供 content 分层内容");
        pools = Map.of("chance",chance,"fortune",fortune,"card",cards);
        for (var entry : pools.entrySet()) validate(entry.getKey(), entry.getValue());
    }
    /** 输入池、可选指定机制与随机源，先抽等级再等概率抽效果和卡面；定向补给只在指定类型的可用等级中抽取。 */
    public ContentResult draw(Room room, String pool, String fixedMechanism, RandomSource random) {
        var definition = pools.get(pool);
        var candidates = candidates(definition, fixedMechanism);
        require(!candidates.isEmpty(), "定向补给机制不可用");
        var rarities = RARITIES.stream().filter(rarity -> candidates.stream().anyMatch(c -> c.parameter.rarity.equals(rarity))).toList();
        String rarity = weighted(rarities, RARITY::get, random);
        var options = candidates.stream().filter(c -> c.parameter.rarity.equals(rarity)).toList();
        var selected = options.get(random.nextInt(options.size()));
        var parameter = selected.parameter;
        var skin = parameter.skins.get(random.nextInt(parameter.skins.size()));
        ContentResult result = new ContentResult(); result.id = random.id(); result.pool = pool; result.mechanism = selected.mechanism;
        result.parameterId = parameter.id; result.skinId = skin.id; result.name = skin.name; result.rarity = rarity; result.quote = skin.quote; result.description = skin.description; result.cardType=skin.cardType;
        parameter.values.forEach((key,value) -> result.parameters.put(key, CASH.contains(key) ? Rules.economy(room).money(moneyKey(pool, selected.mechanism, parameter.id, key), value) : value));
        result.privateSupply = Set.of("cards","cards_one","cards_two","directed").contains(selected.mechanism);
        result.effect = describe(result); return result;
    }
    /** 输入已校验池与可选指定类型，返回全部启用的完整效果组合，无皮肤重复项。 */
    private static List<Candidate> candidates(ContentDefinition definition, String fixedMechanism) {
        return definition.mechanisms.stream().filter(m -> m.enabled && (fixedMechanism == null || m.id.equals(fixedMechanism)))
                .flatMap(m -> m.groups.stream().flatMap(g -> g.parameters.stream().map(p -> new Candidate(m.id, p)))).toList();
    }
    /** 输入新局经济快照，冻结全部内容现金档及手牌弃置折现，重启不采用新倍率。 */
    public void freeze(Room room) {
        for (var entry : pools.entrySet()) for (var mechanism : entry.getValue().mechanisms) for (var group : mechanism.groups) for (var parameter : group.parameters)
            for (var value : parameter.values.entrySet()) if (CASH.contains(value.getKey())) {
                String key = moneyKey(entry.getKey(), mechanism.id, parameter.id, value.getKey());
                room.economy.fixedAmounts.put(key, room.economy.money(key, value.getValue()));
            }
        room.economy.fixedAmounts.put("handDiscard", room.economy.money("handDiscard",200));
    }
    /** 输入池、机制、完整参数及金额字段，返回不依赖皮肤的稳定覆盖键。 */
    public static String moneyKey(String pool, String mechanism, String parameter, String field) { return "content."+pool+"."+mechanism+"."+parameter+"."+field; }
    /** 输入内容结果，返回由实际金额及规则生成的最终卡面效果。 */
    public static String describe(ContentResult r) {
        var p = r.parameters; long amount = p.getOrDefault("amount",0L), steps = p.getOrDefault("steps",0L);
        return switch (r.mechanism) {
            case "income","cash" -> "获得现金 ¥"+amount;
            case "fee" -> "向银行支付 ¥"+amount;
            case "fixed" -> "替代掷骰，移动 "+steps+" 格并结算落点";
            case "remote" -> "自选 1～6 点，替代掷骰并结算落点";
            case "move","forward","back" -> (steps < 0 ? "后退 " : "前进 ")+Math.abs(steps)+" 格并结算落点"+(r.pool.equals("card") ? "，替代本次掷骰" : "");
            case "bonus" -> "掷骰前加码 +"+steps+" 格；合成一次连续移动";
            case "barrier" -> "前方 1～6 格任意无路障地块放置路障，首个受影响者停下";
            case "demolish" -> "对手的一块地产降低 1 级建筑，无退款";
            case "swap" -> "与未服刑的对手交换位置，随后正常掷骰";
            case "immunity" -> "免除本笔租金，不占主动次数";
            case "freeUpgrade" -> "自己的合法地产免费升 1 级；确认前取消保留卡";
            case "halfPayment" -> "本笔强制费用减免 50%（减免额向下取整），不占主动次数";
            case "release" -> "监禁或休息刚施加时可解除对应限制，不占主动次数";
            case "steal" -> "随机转移所选对手的 1 张手牌；对方无牌时保留此卡和主动次数";
            case "cards","cards_one","cards_two" -> "获得私人道具补给";
            case "directed" -> "定向获得 1 张"+List.of("免租卡","免费升级卡","遥控骰子").get(p.get("cardType").intValue());
            case "skip" -> "休息 "+p.get("count")+" 回合；可在施加时使用解控卡";
            case "all_income" -> "所有未破产玩家各获得 ¥"+amount;
            case "all_fee" -> "所有未破产玩家各向银行支付 ¥"+amount;
            case "rich_fee" -> "事件开始时锁定的净资产最高者向银行支付 ¥"+amount;
            case "bottom_income" -> "事件开始时锁定的净资产最低者获得 ¥"+amount;
            case "bottom_bonus" -> "落格者获得 ¥"+amount+"；事件开始时锁定的榜底另得 ¥"+p.get("bonus");
            case "gamble" -> "可放弃，或用现有现金下注 ¥"+p.get("wager")+"；"+p.get("probability")+"% 概率获得 ¥"+p.get("prize")+"（先扣本金，不另返还）";
            case "self_destroy" -> "选择自己的有建筑地产，摧毁其全部建筑；实际损失有 30% 概率获得 50% 补偿";
            case "disaster" -> "随机摧毁地图一块有建筑地产的全部建筑；实际损失有 30% 概率获得 50% 补偿";
            case "free_upgrade" -> "自己的合法地产免费升 1 级；可放弃，无合法目标时获得 ¥"+p.get("fallback");
            case "trap_immunity" -> "获得陷阱免疫，持续 3 个后续个人回合";
            case "next_waiver" -> "自动免除下一笔强制费用，保留到消耗，重复不叠加";
            case "rent_double" -> "下次实际需付租金翻倍，消耗且不叠加";
            case "extra_action" -> "首版停用";
            default -> throw new IllegalStateException("未知内容机制");
        };
    }
    /** 输入已加载池，校验非空分层、参数唯一、合法范围及所有皮肤身份。 */
    private static void validate(String pool, ContentDefinition definition) {
        require(definition != null && definition.mechanisms != null && !definition.mechanisms.isEmpty(), "内容池不能为空");
        Set<String> ids = new HashSet<>(), skins = new HashSet<>();
        for (var m : definition.mechanisms) {
            require((pool.equals("card") ? CARD : EVENT).contains(m.id) && ids.add(m.id) && m.groups != null && !m.groups.isEmpty(), "内容机制不可用或重复");
            require(!m.id.equals("extra_action") || !m.enabled, "再次行动模板首版须停用");
            Set<Map<String,Long>> combinations = new HashSet<>(); Set<String> parameters = new HashSet<>();
            for (var g : m.groups) {
                require(g.parameters != null && !g.parameters.isEmpty(), "参数档不能为空");
                for (var p : g.parameters) {
                    require(p.id != null && parameters.add(p.id) && p.rarity != null && RARITY.containsKey(p.rarity) && p.values != null && combinations.add(p.values) && p.skins != null && !p.skins.isEmpty(), "完整参数重复、影响等级无效或缺少皮肤");
                    validateValues(m.id,p.values);
                    for (var s : p.skins) require(s.id != null && s.id.startsWith(pool+"_") && skins.add(s.id) && p.rarity.equals(s.rarity) && s.name != null && s.quote != null && s.description != null, "皮肤资料不可用、等级不一致或身份重复");
                }
            }
        }
        var enabled = candidates(definition, null);
        for (String rarity : RARITIES) require(enabled.stream().anyMatch(c -> c.parameter.rarity.equals(rarity)), pool+" 缺少启用的 "+rarity+" 效果，无法保持四档概率");
    }
    /** 输入机制及完整参数，核对其字段与已支持玩法范围，不允许皮肤夹带效果。 */
    private static void validateValues(String mechanism, Map<String,Long> p) {
        Set<String> fields = switch (mechanism) {
            case "income","cash","fee","all_income","all_fee","rich_fee","bottom_income" -> Set.of("amount");
            case "fixed","move","bonus","forward","back" -> Set.of("steps");
            case "skip","cards","cards_one","cards_two","trap_immunity" -> Set.of("count");
            case "directed" -> Set.of("cardType");
            case "gamble" -> Set.of("wager","probability","prize");
            case "free_upgrade" -> Set.of("fallback");
            case "bottom_bonus" -> Set.of("amount","bonus");
            default -> Set.of();
        };
        require(p.keySet().equals(fields) && p.values().stream().allMatch(Objects::nonNull), "机制完整参数字段不正确");
        for (var e : p.entrySet()) require(CASH.contains(e.getKey()) ? e.getValue() >= 0 && e.getValue() <= 9007199254740991L : e.getValue() >= -40 && e.getValue() <= (e.getKey().equals("probability") ? 100 : 40), "内容参数越界");
        if (Set.of("fixed","move","bonus","forward","back").contains(mechanism)) {
            long steps=p.get("steps");
            require(switch(mechanism) { case "fixed" -> steps>=0 && steps<=6; case "move" -> steps>=-3 && steps<=6; case "bonus" -> steps>=1 && steps<=3; case "forward" -> steps>=1 && steps<=40; default -> steps<=-1 && steps>=-40; }, "机制移动参数不可用");
        }
        if (p.containsKey("count")) require(p.get("count")>=1 && p.get("count")<=32,"次数参数不可用");
        if (mechanism.equals("trap_immunity")) require(p.get("count")==3,"陷阱免疫须为三个后续个人回合");
        if (p.containsKey("probability")) require(p.get("probability")>=0 && p.get("probability")<=100,"中奖概率不可用");
        if (mechanism.equals("directed")) require(p.get("cardType") >= 0 && p.get("cardType") <= 2, "定向补给类型不可用");
    }
    /** 输入候选项及正整数权重，返回一次加权结果；零权重候选不出货。 */
    private static <T> T weighted(List<T> values, ToIntFunction<T> weight, RandomSource random) {
        int remaining = random.nextInt(values.stream().mapToInt(weight).sum());
        for (T value : values) { remaining -= weight.applyAsInt(value); if (remaining < 0) return value; }
        throw new IllegalStateException("抽取权重不可用");
    }
}
