package com.dfw.domain.rules;

import com.dfw.domain.*;
import java.util.*;
import java.util.function.ToIntFunction;
import static com.dfw.domain.GameError.require;

/** 启动校验内容层，按完整效果权重抽取匹配卡面，结果进入房间快照。 */
public final class ContentCatalog {
    private static final List<String> RARITIES = List.of("R","SR","SSR","UR");
    private static final Map<String, Integer> RARITY = Map.of("R",40,"SR",30,"SSR",25,"UR",5);
    private static final Set<String> CASH = Set.of("amount","wager","prize","bonus","fallback");
    private static final Set<String> CARD = Set.of("fixed","remote","barrier","demolish","fullDemolish","swap","blast","jail","immunity","move","bonus","freeUpgrade","fullUpgrade","halfPayment","release","steal","cash","gamble","shield","recycle","discover","sap","exchange","slow","swapAny","kamui","handDestruction","gravity","reaper","shadowstep","homeTeleport","blackhole","meteor");
    private static final Set<String> EVENT = Set.of("income","fee","forward","back","skip","jail","cards","cards_one","cards_two","directed","all_income","all_fee","rich_fee","bottom_income","gamble","self_destroy","opponent_destroy","disaster","free_upgrade","full_upgrade","trap_immunity","next_waiver","rent_double","bottom_bonus","extra_action","all_morale","talent","global_demolition","treasure","concert","shuffle","all_cards");
    private final Map<String, ContentDefinition> pools;
    /** 一个去重后的机制与完整参数组合，名称和插画不增加其概率。 */
    private record Candidate(String mechanism, ContentDefinition.Parameter parameter) {}
    /** 输入三类文件内容，启动时核对概率、去重参数及皮肤身份，返回唯一内容目录。 */
    public ContentCatalog(ContentDefinition chance, ContentDefinition fortune, ContentDefinition cards) {
        require(chance!=null && fortune!=null && cards!=null,"三份事件文件均须提供 content 分层内容");
        pools = Map.of("chance",chance,"fortune",fortune,"card",cards);
        for (var entry : pools.entrySet()) validate(entry.getKey(), entry.getValue());
    }
    /** 输入池、可选指定机制与随机源，按完整效果权重抽取再等概率选皮肤；定向补给在指定机制内归一化。 */
    public ContentResult draw(Room room, String pool, String fixedMechanism, RandomSource random) {
        return draw(room,pool,fixedMechanism,random,Set.of());
    }
    /** 输入已排除的完整效果，按剩余权重抽取，避免发现出现同效果不同皮肤。 */
    private ContentResult draw(Room room, String pool, String fixedMechanism, RandomSource random, Set<String> excluded) {
        var definition = pools.get(pool);
        var candidates = candidates(definition, fixedMechanism).stream().filter(c -> !excluded.contains(c.mechanism + ":" + c.parameter.id)).toList();
        require(!candidates.isEmpty(), "定向补给机制不可用");
        Candidate selected;
        if (candidates.getFirst().parameter.weight != null) selected = weighted(candidates, c -> c.parameter.weight, random);
        else {
            var rarities = RARITIES.stream().filter(rarity -> candidates.stream().anyMatch(c -> c.parameter.rarity.equals(rarity))).toList();
            String rarity = weighted(rarities, RARITY::get, random);
            var options = candidates.stream().filter(c -> c.parameter.rarity.equals(rarity)).toList();
            selected = options.get(random.nextInt(options.size()));
        }
        var parameter = selected.parameter;
        var skin = parameter.skins.get(random.nextInt(parameter.skins.size()));
        ContentResult result = new ContentResult(); result.id = random.id(); result.pool = pool; result.mechanism = selected.mechanism;
        result.parameterId = parameter.id; result.skinId = skin.id; result.name = skin.name; result.rarity = parameter.rarity; result.quote = skin.quote; result.description = skin.description; result.cardType=skin.cardType;
        parameter.values.forEach((key,value) -> result.parameters.put(key, CASH.contains(key) ? cashValue(room,pool,selected.mechanism,parameter.id,key,value) : value));
        result.privateSupply = Set.of("cards","cards_one","cards_two","directed").contains(selected.mechanism);
        result.effect = describe(result); return result;
    }
    /** 输入房间和随机源，返回三个不同完整效果的固定发现候选。 */
    public List<ContentResult> discover(Room room, RandomSource random) {
        List<ContentResult> result=new ArrayList<>(); Set<String> excluded=new HashSet<>();
        for(int i=0;i<3;i++) { ContentResult card=draw(room,"card",null,random,excluded); result.add(card); excluded.add(card.mechanism+":"+card.parameterId); }
        return result;
    }
    /** 输入已校验池与可选指定类型，返回全部启用的完整效果组合，无皮肤重复项。 */
    private static List<Candidate> candidates(ContentDefinition definition, String fixedMechanism) {
        return definition.mechanisms.stream().filter(m -> m.enabled && (fixedMechanism == null || m.id.equals(fixedMechanism)))
                .flatMap(m -> m.groups.stream().flatMap(g -> g.parameters.stream().map(p -> new Candidate(m.id, p)))).toList();
    }
    /** 输入新局经济快照，冻结现金档及手牌弃置折现；人才引进生活费按实际金额，不乘倍率。 */
    public void freeze(Room room) {
        for (var entry : pools.entrySet()) for (var mechanism : entry.getValue().mechanisms) for (var group : mechanism.groups) for (var parameter : group.parameters)
            for (var value : parameter.values.entrySet()) if (CASH.contains(value.getKey())) {
                String key = moneyKey(entry.getKey(), mechanism.id, parameter.id, value.getKey());
                room.economy.fixedAmounts.put(key, cashValue(room,entry.getKey(),mechanism.id,parameter.id,value.getKey(),value.getValue()));
            }
        room.economy.fixedAmounts.put("handDiscard", room.economy.money("handDiscard",200));
    }
    /** 输入池、机制、完整参数及金额字段，返回不依赖皮肤的稳定覆盖键。 */
    public static String moneyKey(String pool, String mechanism, String parameter, String field) { return "content."+pool+"."+mechanism+"."+parameter+"."+field; }
    /** 输入完整内容金额身份及数值，返回冻结或覆盖金额；人才生活费保持约定的实际金额。 */
    private static long cashValue(Room room, String pool, String mechanism, String parameter, String field, long base) {
        String key=moneyKey(pool,mechanism,parameter,field);
        return mechanism.equals("talent") ? Rules.economy(room).derived(key,base) : Rules.economy(room).money(key,base);
    }
    /** 输入内容结果，返回卡面效果文案；传送和入狱道具使用确认的简写，具体限制仍由执行器校验。 */
    public static String describe(ContentResult r) {
        var p = r.parameters; long amount = p.getOrDefault("amount",0L), steps = p.getOrDefault("steps",0L);
        return switch (r.mechanism) {
            case "income","cash" -> "获得现金 ¥"+amount;
            case "fee" -> "向银行支付 ¥"+amount;
            case "fixed" -> "替代掷骰，移动 "+steps+" 格并结算落点";
            case "remote" -> "自选 1～6 点，替代掷骰并结算落点";
            case "move","forward","back" -> (steps < 0 ? "后退 " : "前进 ")+Math.abs(steps)+" 格并结算落点"+(r.pool.equals("card") ? "，替代本次掷骰" : "");
            case "bonus" -> "掷骰前加码 +"+steps+" 格；合成一次连续移动";
            case "barrier" -> "在附近 6 格内选择一个无路障地块放置路障（前后各 6 格，不含当前格），首个受影响者停下";
            case "demolish" -> "对手的一块地产降低 1 级建筑，无退款";
            case "fullDemolish" -> "指定对手一块有建筑的地产，拆除全部建筑，不发放补偿，保留产权、抵押状态及固定赎回费用；无合法目标时保留卡和主动次数";
            case "swap" -> "与未服刑的对手交换位置，随后正常掷骰";
            case "blast" -> "指定任意一名玩家，直接传送至地图随机地块。";
            case "jail" -> r.pool.equals("card") ? "指定任意一名玩家直接入狱，跳过下一次个人行动。" : "抽到此事件的玩家自己直接入狱，跳过下一次个人行动；服刑期间不收租，其他小游戏正常参与，可在施加时用解控卡解除限制。直接入狱不触发沿途奖励、不消耗塔塔开";
            case "immunity" -> "免除本笔租金，不占主动次数";
            case "freeUpgrade" -> "自己的合法地产免费升 1 级；确认前取消保留卡";
            case "fullUpgrade" -> "选择自己的未抵押、未满级且本回合未升级地产，免费直升 3 级，计入本回合升级；取消或无合法目标时保留卡和主动次数";
            case "halfPayment" -> "本笔强制费用减免 50%（减免额向下取整），不占主动次数";
            case "release" -> "监禁或休息刚施加时可解除对应限制，不占主动次数";
            case "steal" -> "随机转移所选对手的 1 张手牌；对方无牌时保留此卡和主动次数";
            case "cards","cards_one","cards_two" -> "获得私人道具补给";
            case "directed" -> "定向获得 1 张"+List.of("免租卡","免费升级卡","自选骰点道具（任意门）").get(p.get("cardType").intValue());
            case "skip" -> "休息 "+p.get("count")+" 回合；可在施加时使用解控卡";
            case "all_income" -> "所有未破产玩家各获得 ¥"+amount;
            case "all_fee" -> "所有未破产玩家各向银行支付 ¥"+amount;
            case "rich_fee" -> "事件开始时锁定的净资产最高者向银行支付 ¥"+amount;
            case "bottom_income" -> "事件开始时锁定的净资产最低者获得 ¥"+amount;
            case "bottom_bonus" -> "落格者获得 ¥"+amount+"；事件开始时锁定的榜底另得 ¥"+p.get("bonus");
            case "gamble" -> "可放弃，或用现有现金下注 ¥"+p.get("wager")+"；"+p.get("probability")+"% 概率获得 ¥"+p.get("prize")+"（先扣本金，不另返还）";
            case "self_destroy" -> "选择自己的有建筑地产，摧毁其全部建筑；有 50% 概率获得摧毁前单次租金的 50% 补偿（向下取整，抵押地产租金为 0）";
            case "opponent_destroy" -> "选择对手一块有建筑的地产，摧毁全部建筑并保留产权；有 50% 概率向受损业主补偿摧毁前单次租金的 50%（向下取整，抵押地产租金为 0）；无合法目标则结束";
            case "disaster" -> "随机摧毁地图一块有建筑地产的全部建筑；有 50% 概率获得摧毁前单次租金的 50% 补偿（向下取整，抵押地产租金为 0）";
            case "free_upgrade" -> "自己的合法地产免费升 1 级；可放弃，无合法目标时获得 ¥"+p.get("fallback");
            case "full_upgrade" -> "选择自己的未抵押、未满级且本回合未升级地产，免费直升 3 级，计入本回合升级；可放弃，无合法目标时获得 ¥"+p.get("fallback");
            case "all_morale" -> "全体未破产玩家获得塔塔开：下一次最终移动距离×2，生效后消耗，重复不叠加；传送、换位不受影响";
            case "talent" -> "随机获得一块无主地产、生活费 ¥"+amount+" 和 "+p.get("count")+" 张随机道具；无无主地产时只跳过土地发放";
            case "global_demolition" -> "全地图所有有建筑地产（含自己）各降低1级，分别向业主补偿降级前单次租金的"+p.get("compensationPercent")+"%（向下取整，抵押地产补偿为0）；保留产权和抵押状态";
            case "trap_immunity" -> "获得陷阱免疫，持续 3 个后续个人回合";
            case "next_waiver" -> "自动免除下一笔强制费用，保留到消耗，重复不叠加";
            case "rent_double" -> "下次实际需付租金翻倍，消耗且不叠加";
            case "shield" -> "为自家地产附加一次 A.T.力场，抵挡下一次所有类型建筑拆除或降级；主动卖楼及清算除外";
            case "recycle" -> "消耗本牌后，至少选择一张其他手牌弃置，再抽相同数量；弃牌不折现，取消保留卡和主动次数";
            case "discover" -> "从三个不同完整效果的随机道具中选择一张加入手牌；候选仅本人可见，确认前取消保留卡";
            case "sap" -> "将同格的一名对手打晕，休息一回合，并随机偷一张牌（若有）；解控仅解除休息，不撤销偷牌";
            case "exchange" -> "用现有现金向对手支付一笔无修正租金，强制取得该地产；保留建筑、抵押、赎回费用及附加效果";
            case "slow" -> "指定对手，下次掷骰或替代掷骰基础步数减半，向下取整，再处理加码与塔塔开；不叠加";
            case "swapAny" -> "交换任意两名未破产玩家的位置，可包括自己；不结算落点或起点，不解除控制状态";
            case "kamui" -> "将一处地产封入神威，持续施术者三个后续个人回合；所有地产效果暂停，不能经营或受影响，允许破产清算";
            case "handDestruction" -> "所有未破产玩家弃掉全部手牌，等量重抽；使用者数量不包括本牌，弃牌不折现";
            case "gravity" -> "将所有其他未破产玩家吸到自己所在格，不结算落点、起点、路障或活动，随后仍正常掷骰";
            case "reaper" -> "自己与指定对手的本局手牌上限永久各减一，可叠加且最低一张；超限按原规则整理";
            case "shadowstep" -> "替代本次掷骰，传送到另一名玩家所在格并正常结算；到起点领取一次现金及道具，不走沿途";
            case "homeTeleport" -> "替代本次掷骰，传送到自有地产并正常结算，可按原规则付费升级；不能传送到神威地产";
            case "blackhole" -> "清除一处地产上的护盾、宝藏、演唱会、路障及陨星；不改变建筑、产权、抵押，不影响神威";
            case "meteor" -> "随机在一至三块不同地产各放一颗陨星；每颗截停一次，可与路障或已有陨星叠加，不损坏建筑";
            case "treasure" -> "随机标记一处地产，首位正常停留者获得等于当前无修正租金的宝藏；此次免租且不消耗租金状态，无主按零级";
            case "concert" -> "随机选五处不同地产，各叠加一层演唱会；下次正常正租金每层乘1.2，触发时全部消耗，之后防御仍消耗";
            case "shuffle" -> "所有未破产玩家随机接手另一人的原位置，不结算落点或起点，不消耗移动状态";
            case "all_cards" -> "所有未破产玩家各获得一张私人随机道具，补给后按个人当前上限整理";
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
        if (enabled.stream().anyMatch(c -> c.parameter.weight != null)) {
            require(enabled.stream().allMatch(c -> c.parameter.weight != null && c.parameter.weight > 0), pool+" 启用效果须全部配置正整数权重");
            require(enabled.stream().mapToLong(c -> c.parameter.weight).sum() <= Integer.MAX_VALUE, pool+" 抽取总权重过大");
        }
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
            case "free_upgrade","full_upgrade" -> Set.of("fallback");
            case "bottom_bonus" -> Set.of("amount","bonus");
            case "talent" -> Set.of("amount","count");
            case "global_demolition" -> Set.of("compensationPercent");
            default -> Set.of();
        };
        require(p.keySet().equals(fields) && p.values().stream().allMatch(Objects::nonNull), "机制完整参数字段不正确");
        for (var e : p.entrySet()) require(CASH.contains(e.getKey()) ? e.getValue() >= 0 && e.getValue() <= 9007199254740991L : e.getValue() >= -40 && e.getValue() <= (Set.of("probability","compensationPercent").contains(e.getKey()) ? 100 : 40), "内容参数越界");
        if (Set.of("fixed","move","bonus","forward","back").contains(mechanism)) {
            long steps=p.get("steps");
            require(switch(mechanism) { case "fixed" -> steps>=0 && steps<=6; case "move" -> steps>=-3 && steps<=6; case "bonus" -> steps>=1 && steps<=3; case "forward" -> steps>=1 && steps<=40; default -> steps<=-1 && steps>=-40; }, "机制移动参数不可用");
        }
        if (p.containsKey("count")) require(p.get("count")>=1 && p.get("count")<=32,"次数参数不可用");
        if (mechanism.equals("trap_immunity")) require(p.get("count")==3,"陷阱免疫须为三个后续个人回合");
        if (p.containsKey("probability")) require(p.get("probability")>=0 && p.get("probability")<=100,"中奖概率不可用");
        if (p.containsKey("compensationPercent")) require(p.get("compensationPercent")>=0 && p.get("compensationPercent")<=100,"补偿比例不可用");
        if (mechanism.equals("directed")) require(p.get("cardType") >= 0 && p.get("cardType") <= 2, "定向补给类型不可用");
    }
    /** 输入候选项及正整数权重，返回一次加权结果；零权重候选不出货。 */
    private static <T> T weighted(List<T> values, ToIntFunction<T> weight, RandomSource random) {
        int remaining = random.nextInt(values.stream().mapToInt(weight).sum());
        for (T value : values) { remaining -= weight.applyAsInt(value); if (remaining < 0) return value; }
        throw new IllegalStateException("抽取权重不可用");
    }
}
