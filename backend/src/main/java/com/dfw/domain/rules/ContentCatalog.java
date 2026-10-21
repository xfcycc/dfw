package com.dfw.domain.rules;

import com.dfw.domain.*;
import java.util.*;
import java.util.function.ToLongFunction;
import static com.dfw.domain.GameError.require;

/** 启动校验内容层，道具按档位及完整效果均分，事件按权重抽取，结果进入房间快照。 */
public final class ContentCatalog {
    private static final List<String> RARITIES = List.of("R","SR","SSR","UR");
    private static final Map<String, Integer> RARITY = Map.of("R",40,"SR",30,"SSR",25,"UR",5);
    /** 保留现有道具四档总出率，档内效果数量和卡面数量不改变档位概率。 */
    private static final Map<String, Integer> CARD_RARITY = Map.of("R",57087,"SR",102383,"SSR",183480,"UR",18050);
    private static final Set<String> CASH = Set.of("amount","wager","prize","bonus","fallback");
    private static final Set<String> CARD = Set.of("xiaowuSacrifice","asunaFlash","defenseSpecialization","revealingSwords","fusion","shadowBlade","doubleBoost","blueSilverBind","goldenCore","nuwaMend","namelessSword","kingsPressure","keqingRush","thunderLord","hollowfication","fireBurial","bankai","igniteSea","senbonzakura","geoResonance","growthLink","sereniteaPot","createBuild","remoteConstruction","shrineGrowth","fixed","remote","barrier","demolish","fullDemolish","swap","blast","jail","immunity","move","bonus","freeUpgrade","fullUpgrade","halfPayment","release","steal","cash","gamble","shield","recycle","discover","sap","exchange","slow","swapAny","kamui","handDestruction","gravity","reaper","shadowstep","homeTeleport","blackhole","meteor","timeRewind","projection","starburst","imagineBreaker","timeRestore","vectorReflect","deathReturn","explosion","corruption","contract","commandSpell","sevenSwords","railgun","sealCard","swordRain","voidTears","absoluteZero");
    private static final Set<String> EVENT = Set.of("aquaPurify","worldSeed","darkGame","nzothWrath","endlessClouds","pinkMiracle","igniteSea","geniusClub","senbonzakura","allConstruction","erdtreeBlessing","income","fee","forward","back","skip","jail","cards","cards_one","cards_two","directed","all_income","all_fee","rich_fee","bottom_income","gamble","self_destroy","opponent_destroy","disaster","free_upgrade","full_upgrade","trap_immunity","next_waiver","rent_double","bottom_bonus","extra_action","all_morale","talent","global_demolition","treasure","concert","shuffle","all_cards","classTrial");
    private final Map<String, ContentDefinition> pools;
    /** 一个去重后的机制与完整参数组合，名称和插画不增加其概率。 */
    private record Candidate(String mechanism, ContentDefinition.Parameter parameter) {}
    /** 输入三类文件内容，启动时核对概率、去重参数及皮肤身份，返回唯一内容目录。 */
    public ContentCatalog(ContentDefinition chance, ContentDefinition fortune, ContentDefinition cards) {
        require(chance!=null && fortune!=null && cards!=null,"三份事件文件均须提供 content 分层内容");
        pools = Map.of("chance",chance,"fortune",fortune,"card",cards);
        for (var entry : pools.entrySet()) validate(entry.getKey(), entry.getValue());
    }
    /** 输入池、可选机制与随机源，道具先抽档位再均分完整效果，事件按权重抽取，最后等概率选卡面。 */
    public ContentResult draw(Room room, String pool, String fixedMechanism, RandomSource random) {
        return draw(room,pool,fixedMechanism,random,Set.of());
    }
    /** 输入已排除的完整效果，按剩余可选档位或事件权重抽取，避免发现出现同效果不同卡面。 */
    private ContentResult draw(Room room, String pool, String fixedMechanism, RandomSource random, Set<String> excluded) {
        var definition = pools.get(pool);
        var candidates = candidates(definition, fixedMechanism).stream().filter(c -> !excluded.contains(c.mechanism + ":" + c.parameter.id)).toList();
        require(!candidates.isEmpty(), "定向补给机制不可用");
        Candidate selected;
        if (!pool.equals("card") && candidates.getFirst().parameter.weight != null) selected = weighted(candidates, c -> c.parameter.weight, random);
        else {
            var rarities = RARITIES.stream().filter(rarity -> candidates.stream().anyMatch(c -> c.parameter.rarity.equals(rarity))).toList();
            var weights = pool.equals("card") ? CARD_RARITY : RARITY;
            String rarity = weighted(rarities, weights::get, random);
            var options = candidates.stream().filter(c -> c.parameter.rarity.equals(rarity)).toList();
            selected = options.get(random.nextInt(options.size()));
        }
        return materialize(room,pool,selected,random);
    }
    /** 输入档位、可选玩法类别和已排除效果，均分抽取固定档位完整效果并冻结卡面。 */
    public ContentResult drawTier(Room room,String rarity,String category,Set<String> excluded,RandomSource random) {
        var options=candidates(pools.get("card"),null).stream().filter(c -> c.parameter.rarity.equals(rarity) && !excluded.contains(c.mechanism+":"+c.parameter.id)).toList();
        if(category!=null) {
            var matching=options.stream().filter(c -> CrossoverService.category(c.mechanism).equals(category)).toList();
            if(!matching.isEmpty()) options=matching;
        }
        require(!options.isEmpty(),"该档位没有可抽取的完整效果");
        return materialize(room,"card",options.get(random.nextInt(options.size())),random);
    }
    /** 输入固定完整效果，生成新的冻结结果，皮肤数量不改变机制概率。 */
    private ContentResult materialize(Room room,String pool,Candidate selected,RandomSource random) {
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
    /** 输入房间与随机源，排除UR后按原档位权重抽取三个不同完整效果的公共候选。 */
    public List<ContentResult> research(Room room,RandomSource random) {
        Set<String> excluded=new HashSet<>(); List<ContentResult> result=new ArrayList<>();
        for(Candidate candidate:candidates(pools.get("card"),null)) if(candidate.parameter.rarity.equals("UR")) excluded.add(candidate.mechanism+":"+candidate.parameter.id);
        for(int i=0;i<3;i++) { ContentResult card=draw(room,"card",null,random,excluded); result.add(card); excluded.add(card.mechanism+":"+card.parameterId); }
        return result;
    }
    /** 输入房间与随机源，固定三个互不重复的UR完整效果，候选皮肤只在效果选定后抽取。 */
    public List<ContentResult> researchUr(Room room,RandomSource random) {
        Set<String> excluded=new HashSet<>(); List<ContentResult> result=new ArrayList<>();
        for(int i=0;i<3;i++) { ContentResult card=drawTier(room,"UR",null,excluded,random); result.add(card); excluded.add(card.mechanism+":"+card.parameterId); }
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
    /** 输入完整金额身份及数值，返回冻结或覆盖金额；人才生活费及新版命运硬币按实际金额，避免旧局新抽卡重复乘倍率。 */
    private static long cashValue(Room room, String pool, String mechanism, String parameter, String field, long base) {
        String key=moneyKey(pool,mechanism,parameter,field);
        boolean fixedCoin=pool.equals("card") && mechanism.equals("gamble") && parameter.equals("prize-20000_probability-50_wager-5000");
        return mechanism.equals("talent") || fixedCoin ? Rules.economy(room).derived(key,base) : Rules.economy(room).money(key,base);
    }
    /** 输入含冻结参数的卡面，返回统一效果说明；保留范围、代价及特殊结算，通用选中限制由交互提示。 */
    public static String describe(ContentResult r) {
        var p = r.parameters; long amount = p.getOrDefault("amount",0L), steps = p.getOrDefault("steps",0L);
        return switch (r.mechanism) {
            case "asunaFlash" -> "自选向前突进1～3格，替代掷骰；忽略路径效果并清除沿途附加效果，每处护盾只削1层，神威除外；不降建筑，正常结算终点，可被通常主动防御响应。";
            case "aquaPurify" -> "随机清除3处未封印地产的全部正负面附加效果，不改变建筑、产权、抵押或债务。";
            case "worldSeed" -> "随机选择2处不同的合法自家地产，各免费升1级，计入本回合升级。";
            case "defenseSpecialization" -> "本回合及下个本人回合，抵消敌方主动牌对自己及自家地产的全部效果，含UR、不限次数；基础移动固定1，不能替代移动或主动攻击。不免租、不免债、不挡公共事件、不清已有负面状态。";
            case "darkGame" -> "邀请对手自愿各暗押1张非临时手牌，可押UR；各掷1次对决骰，大者获得两张牌，平局退回。对决不移动，拒绝即结束。";
            case "revealingSwords" -> "指定1名对手，接下来3个个人回合禁止使用所有手牌，包含防御、解控和救援响应；掷骰、经营、收租、交易及弃牌照常，重复刷新不叠加。";
            case "fusion" -> "消耗本牌及额外任意2张手牌，融合为1张普通UR；优先在第一张材料的玩法类别内均分抽取，无同类UR才抽全部UR，不额外赠送使用次数。";
            case "shadowBlade" -> "选择2处紧邻、不同的对手有建筑地产，各降1级；可属于不同对手，护盾逐地抵挡，不退款，不能集火。";
            case "nzothWrath" -> "随机最多3名有建筑玩家各1处地产受污染；业主仅本人回合可弃1牌净化。每次本人回合结束先蚀1层护盾，无盾再降1级，持续到护盾与等级归零，神威期间暂停。";
            case "doubleBoost" -> "普通掷骰前使用；骰子移动结束或被截停后，可依次追加两次前进1格。喷射忽略且不消耗陨星，路障仍截停并终止喷射；只结算最终落点，喷射不受翻倍与加码放大。";
            case "blueSilverBind" -> "前后6格内1名对手下个个人回合不能掷骰或替代移动；可使用其他牌、经营、交易、收租与防御，不重新结算原地，可解控，沿用连续控制保护。";
            case "goldenCore" -> "掷骰前闭关，放弃本回合主动移动；回合结束结丹，手牌上限永久加1，并获得1张随机SSR道具。经营、交易及防御照常，每人每局限成功1次。";
            case "nuwaMend" -> "下个本人回合结束前，修复最近同一次敌方攻击或灾害损坏的最多3处自家地产，恢复损坏前等级；产权须未变，不恢复主动卖房，不回滚付款、抵押和其他状态。";
            case "endlessClouds" -> "全体未破产玩家各获得实际现金400；仅踩格者清除本人及自家地产全部临时负面状态，包含敌方神威，不清债务、永久变化或自愿代价。";
            case "namelessSword" -> "本回合蓄力，无法主动移动；下个本人回合掷骰前可对前后6格内1处对手地产连斩3次，各降1级、护盾逐击挡。释放不占主动次数，次回合正常移动；休息、监禁或放弃则失效。";
            case "pinkMiracle" -> "选择最多3张任意手牌重塑，每张从3个同稀有度、不同效果候选中选1，不升级、不增牌；包含UR，保留临时期限、已用次数与冷却。无手牌时获得实际现金800。";
            case "kingsPressure" -> p.getOrDefault("version",0L)>=2 ? "指定1名对手，接下来3个个人回合禁止发动所有等级的主动攻击；非攻击主动牌及所有等级防御、解控、救援响应照常。重复刷新不叠加。" : "指定1名对手，接下来3个个人回合禁止使用UR主动牌；其他等级主动牌及所有等级防御、解控、救援响应照常。重复刷新不叠加。";

            case "keqingRush" -> "对前后6格内1处对手有建筑地产降1级，再传送到该地并正常结算，替代掷骰；护盾可抵挡降级，主动防御不取消自己的传送。";
            case "thunderLord" -> "标记1处对手有建筑地产，初始2击；每名其他玩家首次消耗主动次数用牌增加1击，最多4击，下个本人回合开始逐击降级；护盾逐击抵挡，标记可清除，转手或施术者淘汰失效。";
            case "hollowfication" -> "抵消1张敌方主动道具对本人的强制位移、休息和监禁；不保护地产或手牌，不抵挡事件，不占主动次数。";
            case "fireBurial" -> "献祭自家1级建筑，使对手1处地产降低最多2级，无视但不消耗护盾；献祭无退款、不可修复，被主动防御也不返还。";
            case "xiaowuSacrifice" -> "掷骰前献祭2处不同的自家未抵押、未封印地产，失去土地及全部建筑，无退款且不可修复；获得十万年魂环：含发动回合共8个本人回合内，最多自主免除3笔正租金，见到最终金额后选择。仅免租，不免税费罚款；正面增益不被净化或解控清除，存续期间不可重复发动。";
            case "bankai" -> p.getOrDefault("version",0L)>=2 ? "发动后，本回合最多再使用2张其他主动道具，含UR；期间不能再用天锁斩月或投影魔术，额外出牌统一计数；替代移动后结束出牌窗口，费用和限制照常。" : "固定掷出两枚六面骰，选其中一点前进或后退，替代掷骰；只移动结算1次，不能取消刷点，不适用时间溯行。";
            case "igniteSea" -> p.getOrDefault("version",0L)>=2 ? "选择1处对手有建筑、未封印地产，先移除全部护盾，再降低1＋移除护盾层数级建筑，最多降3级；可被主动防御抵消，保留其他附加效果。" : "可弃置1张自选手牌，选择前进1～6格并正常结算；弃牌不折现，也可放弃，无牌时结束。";
            case "geniusClub" -> p.getOrDefault("version",0L)>=2 ? "公开3张不同完整效果的UR道具；各玩家秘密投入0～2张手牌或放弃，每张计1贡献，触发者额外＋1。最高者胜，并列随机；仅胜者弃置投入牌并自选1张候选，其他人不付代价。" : "公开3张不同完整效果的非UR候选，各玩家秘密选1张或放弃；同项冲突随机1人获得，每项只发1份，具体选择和获牌对应关系保密。";
            case "senbonzakura" -> p.getOrDefault("version",0L)>=2 ? "获得3片攻防共享花瓣，至第3个后续个人回合结束；每回合掷骰前可一次消耗1～3片，各使不同对手地产降1级，不占主动次数；防御时每片挡1次常规建筑破坏，护盾优先，穿盾攻击无视花瓣；重复补至3片但不延长。" : "随机最多3处有建筑、未抵押地产获得花瓣标记，下次正租金按低1级计算后消耗；不改变建筑及估值，至触发者下回合开始到期，转手失效。";

            case "geoResonance" -> "选择1处自家地产，与左右紧邻的自家地产各免费升1级，最多3处。";
            case "growthLink" -> "连接2处自家地产，持续3个后续个人回合；其中一处付费建造时，另一处免费升1级，最多触发2次；免费升级不触发。";
            case "sereniteaPot" -> "重新分配2处自家地产的建筑等级，再选其中一处免费升1级；每处最高3级，均不占本回合升级次数。";
            case "createBuild" -> "弃置1～2张其他手牌，每张换1处不同自家地产免费升1级；弃牌不折现，确认后才消耗。";
            case "remoteConstruction" -> "本回合及下个个人回合，在资产管理阶段各可远程建造1次，支付标准建造费的75%，向下取整。";
            case "shrineGrowth" -> "标记1处自家地产；其他玩家正常停留并结算后，该地产免费升1级，最多触发2次；暂不能升级时保留次数。";
            case "allConstruction" -> "所有玩家各选1处自家地产免费升1级；可放弃，无目标则跳过。";
            case "erdtreeBlessing" -> "自己与建筑总等级最低者各免费升1级，并列随机；若为同一人，可在同一地产连续升级，最高3级。";
            case "income","cash" -> "获得现金 ¥"+amount+"。";
            case "fee" -> "向银行支付 ¥"+amount+"。";
            case "fixed" -> "移动 "+steps+" 格并结算落点，替代掷骰。";
            case "remote" -> "自选1～6点移动并结算落点，替代掷骰。";
            case "move" -> (steps == 0 ? "原地停留并重新结算当前格" : (steps < 0 ? "后退 " : "前进 ")+Math.abs(steps)+" 格并结算落点")+"，替代掷骰。";
            case "forward","back" -> (steps < 0 ? "后退 " : "前进 ")+Math.abs(steps)+" 格并结算落点。";
            case "bonus" -> "本次掷骰额外前进 "+steps+" 格，合并移动并只结算最终落点。";
            case "barrier" -> "在前后6格内放置1处路障，不含当前格；首位受影响的经过者停下并结算落点。";
            case "demolish" -> "对手1处有建筑地产降1级，无退款；护盾可抵挡。";
            case "fullDemolish" -> "拆除对手1处地产的全部建筑，无补偿；无视护盾。";
            case "swap" -> "与1名未服刑的对手交换位置，不结算落点或起点奖励；随后正常掷骰。";
            case "blast" -> "指定1名玩家随机传送并结算落点，可选自己；不发起点奖励，随后正常掷骰。";
            case "jail" -> "card".equals(r.pool) ? "指定1名玩家入狱，可选自己；跳过下次个人行动，服刑期间不收租，可当场解控；对自己使用替代掷骰。" : "自己入狱，跳过下次个人行动，服刑期间不收租，可当场解控；不领取沿途奖励。";
            case "immunity" -> "免除本笔全部租金，不占主动次数。";
            case "freeUpgrade" -> "1处自家地产免费升1级，计入本回合升级；取消或无目标时保留卡和主动次数。";
            case "fullUpgrade" -> "1处自家地产免费直升3级，计入本回合升级；取消或无目标时保留卡和主动次数。";
            case "halfPayment" -> "本笔强制费用减免50%，减免额向下取整；不占主动次数。";
            case "release" -> "解除刚施加的监禁或休息，或抵消1次对自己的强制位移（含入狱传送）；不占主动次数。";
            case "steal" -> "随机抢走指定对手1张手牌；对方无牌时保留卡和主动次数。";
            case "cards","cards_one","cards_two" -> "获得 "+p.get("count")+" 张随机道具。";
            case "directed" -> "获得1张"+List.of("免租卡","免费升级卡","自选骰点道具").get(p.get("cardType").intValue())+"。";
            case "skip" -> "休息 "+p.get("count")+" 个个人回合，可当场解控。";
            case "all_income" -> "所有玩家各获得现金 ¥"+amount+"。";
            case "all_fee" -> "所有玩家各向银行支付 ¥"+amount+"。";
            case "rich_fee" -> "净资产最高者向银行支付 ¥"+amount+"，并列随机。";
            case "bottom_income" -> "净资产最低者获得现金 ¥"+amount+"，并列随机。";
            case "bottom_bonus" -> "自己获得现金 ¥"+amount+"，净资产最低者另得 ¥"+p.get("bonus")+"，并列随机。";
            case "gamble" -> "用现有现金下注 ¥"+p.get("wager")+"，"+p.get("probability")+"%概率获得 ¥"+p.get("prize")+"，本金不返还；取消不消耗。";
            case "self_destroy" -> "选择1处自家地产全拆建筑，护盾可抵挡；业主有50%概率获拆除前租金的50%补偿，向下取整。";
            case "opponent_destroy" -> "选择对手1处地产全拆建筑，护盾可抵挡；业主有50%概率获拆除前租金的50%补偿，向下取整。";
            case "disaster" -> "随机全拆1处地产的建筑，护盾可抵挡；业主有50%概率获拆除前租金的50%补偿，向下取整。";
            case "free_upgrade" -> "1处自家地产免费升1级，计入本回合升级；可放弃，仅无目标时改获现金 ¥"+p.get("fallback")+"。";
            case "full_upgrade" -> "1处自家地产免费直升3级，计入本回合升级；可放弃，仅无目标时改获现金 ¥"+p.get("fallback")+"。";
            case "all_morale" -> "全体玩家下次最终移动距离×2，不叠加；传送、换位不消耗。";
            case "talent" -> "随机获得1块无主地产、现金 ¥"+amount+"及 "+p.get("count")+" 张随机道具；无主地产不足时仅跳过土地奖励。";
            case "global_demolition" -> "随机选1～5处有建筑地产各降1级，包含自家、不重复，护盾可抵挡；逐地补偿业主降级前租金的"+p.get("compensationPercent")+"%，向下取整。";
            case "trap_immunity" -> "获得陷阱免疫，持续3个后续个人回合。";
            case "next_waiver" -> "自动免除下一笔强制费用，不叠加。";
            case "rent_double" -> "下次实际需付的租金翻倍，不叠加。";
            case "shield" -> "为1处自家地产添加 "+p.getOrDefault("charges",1L)+" 层护盾，每层抵挡1次建筑拆除或降级，连击逐层消耗；不叠加，不保护主动出售或清算。";
            case "recycle" -> "选择至少1张其他手牌弃置，等量重抽；弃牌不折现，确认后才消耗。";
            case "discover" -> "从3张不同效果的随机道具中选1张；确认后才消耗，取消重选不刷新候选。";
            case "sap" -> "前后3格内1名对手休息1个个人回合（含同格），并随机偷取其1张手牌；解控不撤销偷牌。";
            case "exchange" -> "用现有现金向对手支付该地产的无修正租金，最低为地价，取得产权并承接建筑、抵押及附加效果。";
            case "slow" -> "指定对手，下次掷骰或替代掷骰的基础步数减半，向下取整，再计算加码与塔塔开；不叠加。";
            case "swapAny" -> "交换任意2名玩家的位置，可含自己；不结算落点或起点奖励，不解除监禁或休息。";
            case "kamui" -> "封印同一条边1～3处地产，无需连续，可含自家、对手或无主地产；持续3个后续个人回合，暂停收租、经营和附加效果，期间不受其他地产效果影响，仍可破产清算。";
            case "handDestruction" -> "全体玩家弃掉剩余手牌并等量重抽，包含自己；本牌不参与重抽，弃牌不折现。";
            case "gravity" -> "将其他所有玩家传送到自己所在格，不结算落点或起点奖励；随后正常掷骰。";
            case "reaper" -> "自己与指定对手的本局手牌上限永久各减1，最低1张；可叠加，超限时选择保留。";
            case "shadowstep" -> "传送到前后6格内另一名玩家所在格并结算，含同格，替代掷骰；传至起点可领取奖励。";
            case "homeTeleport" -> "传送到1处自家地产并结算，替代掷骰，可付费升级。";
            case "blackhole" -> "清除1处地产的全部附加效果，包含护盾、宝藏、演唱会及建设标记；不改变建筑、产权或抵押。";
            case "meteor" -> "随机在1～3处不同地产各放1颗陨星，每颗截停1次并结算落点；可叠加，不损坏建筑。";
            case "treasure" -> "随机标记1处地产；首位正常停留者免租，并获得等于无修正租金的现金；无主地产按0级计算。";
            case "concert" -> "随机5处不同地产各增加1层演唱会；下次正租金每层×1.2，触发时全部消耗，付款防御不返还层数。";
            case "shuffle" -> "所有玩家随机交换位置，每人接手另一人的原位置；不结算落点或起点奖励，不消耗移动状态。";
            case "all_cards" -> "所有玩家各获得1张随机道具。";
            case "timeRewind" -> "普通骰点公开后、移动前重掷1次，必须接受新结果；不适用替代掷骰，不占主动次数。";
            case "projection" -> "复制上个已结束个人回合使用过的1张主动道具，含UR，不含投影魔术；复制牌本回合可额外使用1次，回合结束消失。";
            case "starburst" -> "对手1处有建筑地产连续降级2次，每次降1级；每层护盾抵挡1击，无退款。";
            case "imagineBreaker" -> "抵消1张主动道具对自己及自家地产的全部效果，含UR；群体效果只抵消自己一方，对方仍消耗卡和次数。";
            case "classTrial" -> "所有玩家秘密投给另一人或弃权，最高票者休息1个个人回合，可解控；并列随机，全弃权则无人休息。";
            case "timeRestore" -> "在自己的下个个人回合结束前，将1处自家地产恢复至最近一次敌方或事件破坏前的建筑等级；产权须未变化，不恢复主动卖房或其他状态。";
            case "vectorReflect" -> "将1张主动道具对自己的效果反弹给施术者，含UR；不保护地产，群体只反弹自己部分，反弹不再触发反射。";
            case "deathReturn" -> "达到破产清算条件时，免除当前剩余强制债务，回到最近本人回合开始位置，获得现金 ¥3000并休息1个个人回合；不结算落点或起点奖励，不回滚已完成的付款、卖房或交易。";
            case "explosion" -> "拆除连续3格内的全部建筑，包含自家地产，无补偿；每处地产可消耗1层护盾抵挡全拆；自己随后休息1个个人回合。";
            case "corruption" -> "对手1处有建筑地产，在接下来3次业主个人回合开始时各降1级；每层护盾抵挡1次，降至0级结束，重复施加刷新次数，可清除。";
            case "contract" -> "获得现金 ¥5000，本局手牌上限永久减1，最低1张；上限为1时不可使用，超限时选择保留。";
            case "commandSpell" -> "指定另一名玩家立即移动1～6格，点数自选，正常结算沿途和落点；不占其正常行动，奖励、费用及落点选择归移动者。";
            case "sevenSwords" -> "将3道剑气分配给对手有建筑的地产，可集火或分散，每道降1级；每层护盾抵挡1道，未分配的剑气作废。";
            case "railgun" -> "选择顺或逆时针贯穿前方6格，自己不移动；无视护盾，清除沿途路障、陨星和全部护盾，对手地产各降1级。";
            case "sealCard" -> "抵消并收服另一玩家仅针对自己或1处自家地产的单目标主动道具，含UR；收服后按正常时机使用，不适用群体或范围效果。";
            case "swordRain" -> "前后6格内1名对手休息1个个人回合，可解控；本牌可用2次，每个人回合限1次，被防御也消耗次数，转移不刷新次数或冷却。";
            case "voidTears" -> "连接2处地产；首位步行进入任一端者传至另一端，再走剩余步数，随后裂隙消失；跳过的路径无奖励，只结算最终落点，传送或换位不触发。";
            case "absoluteZero" -> "冰封一条边的全部地产至自己的下个个人回合结束，包含自家地产；期间不能收租或升级，其他效果及资产操作照常。";
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
            require(enabled.stream().map(c -> c.parameter.weight).reduce(0L,Math::addExact) <= 9007199254740991L, pool+" 抽取总权重过大");
        }
        for (String rarity : RARITIES) require(enabled.stream().anyMatch(c -> c.parameter.rarity.equals(rarity)), pool+" 缺少启用的 "+rarity+" 效果，无法保持四档概率");
    }
    /** 输入机制及完整参数，核对其字段与已支持玩法范围，不允许皮肤夹带效果。 */
    private static void validateValues(String mechanism, Map<String,Long> p) {
        Set<String> fields = switch (mechanism) {
            case "bankai","igniteSea","senbonzakura","geniusClub","kingsPressure" -> p.containsKey("version") ? Set.of("version") : Set.of();
            case "shield" -> p.containsKey("charges") ? Set.of("charges") : Set.of();
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
        if(p.containsKey("version")) require(p.get("version")==2,"内容规则版本不可用");
        if (p.containsKey("count")) require(p.get("count")>=1 && p.get("count")<=32,"次数参数不可用");
        if (mechanism.equals("shield") && p.containsKey("charges")) require(p.get("charges")==3,"三重罗生门须为三层护盾");
        if (mechanism.equals("trap_immunity")) require(p.get("count")==3,"陷阱免疫须为三个后续个人回合");
        if (p.containsKey("probability")) require(p.get("probability")>=0 && p.get("probability")<=100,"中奖概率不可用");
        if (p.containsKey("compensationPercent")) require(p.get("compensationPercent")>=0 && p.get("compensationPercent")<=100,"补偿比例不可用");
        if (mechanism.equals("directed")) require(p.get("cardType") >= 0 && p.get("cardType") <= 2, "定向补给类型不可用");
    }
    /** 输入候选项及正整数权重，返回一次加权结果；零权重候选不出货。 */
    private static <T> T weighted(List<T> values, ToLongFunction<T> weight, RandomSource random) {
        long remaining = random.nextLong(values.stream().mapToLong(weight).reduce(0L,Math::addExact));
        for (T value : values) { remaining -= weight.applyAsLong(value); if (remaining < 0) return value; }
        throw new IllegalStateException("抽取权重不可用");
    }
}
