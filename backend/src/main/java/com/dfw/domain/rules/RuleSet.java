package com.dfw.domain.rules;

import com.dfw.domain.*;
import java.util.*;
import java.util.regex.Pattern;
import lombok.Getter;
import static com.dfw.domain.rules.RuleExpression.check;

/** 由四类文件编译的唯一不可变规则集。 */
@Getter
public final class RuleSet {
    private static final Set<String> WINDOW_ACTIONS = Set.of("roll", "card", "tradeOffer", "sellBuilding", "mortgage", "redeem", "sellLand", "endTurn");
    private static final Map<String, Map<String, String>> EFFECT_PARAMS = Map.ofEntries(
            Map.entry("REPLACE_ROLL", Map.of("amount", "NUMBER")), Map.entry("MOVE", Map.of("amount", "NUMBER")),
            Map.entry("SETTLE_TILE", Map.of()), Map.entry("PAY_FEE", Map.of("amount", "NUMBER", "targetId", "STRING", "tileId", "NUMBER")),
            Map.entry("PLACE_BARRIER", Map.of("tileId", "NUMBER")), Map.entry("DEMOLISH", Map.of("tileId", "NUMBER")),
            Map.entry("SWAP_POSITION", Map.of("targetId", "STRING")), Map.entry("WAIVE_RENT", Map.of()), Map.entry("LOG", Map.of()),
            Map.entry("GRANT_MONEY", Map.of("amount", "NUMBER")), Map.entry("DRAW_CARDS", Map.of("amount", "NUMBER")), Map.entry("SKIP_TURN", Map.of("amount", "NUMBER")),
            Map.entry("CHARGE_ALL", Map.of("amount", "NUMBER")), Map.entry("ADD_STATUS", Map.of("statusId", "STRING", "amount", "NUMBER")),
            Map.entry("DESTROY_BUILDINGS", Map.of("tileId", "NUMBER", "selector", "STRING")));
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([a-zA-Z][a-zA-Z0-9_.]*)}");
    /** 分层内容池，与既有阶段 DSL 共用效果队列。 */
    private final ContentCatalog content;
    /** 有序阶段。 */
    private final List<Stage> stages;
    /** 全部等权机会事件。 */
    private final List<EventRule> chanceEvents;
    /** 全部等权财富事件。 */
    private final List<EventRule> fortuneEvents;
    /** 按配置顺序订阅实际灾害建筑损失的规则。 */
    private final List<PassiveRule> passiveRules;
    /** 道具定义索引。 */
    private final Map<String, CardRule> cards;
    /** 输入校验后的阶段、道具、两类事件及订阅，保存不可变集合。 */
    private RuleSet(List<Stage> stages, Map<String, CardRule> cards, List<EventRule> chanceEvents, List<EventRule> fortuneEvents, List<PassiveRule> passiveRules, ContentCatalog content) {
        this.content = content;
        this.stages = List.copyOf(stages); this.cards = Map.copyOf(cards); this.chanceEvents = List.copyOf(chanceEvents); this.fortuneEvents = List.copyOf(fortuneEvents); this.passiveRules = List.copyOf(passiveRules);
    }
    /** 已校验的补偿订阅，参数按事件发生时固定，不依赖执行时产权。 */
    public record PassiveRule(String id, String trigger, String tag, int probability, int compensationPercent) {}
    /** 编译后的自动事件，仅显式声明的扩展描述进入公开说明。 */
    public record EventRule(String id, String description, List<Step> steps) {}
    /** 编译后的阶段，不存储运行游标。 */
    public record Stage(String id, String handler, String hook, String phase, Set<String> actions, Set<String> exitOn, List<Step> steps) {}
    /** 编译后的卡牌发动政策。 */
    public record CardRule(String id, String activation, Set<String> stages, boolean countsAsActive, List<Check> checks, List<Step> steps) {}
    /** 有错误文案的布尔条件。 */
    public record Check(RuleExpression.Node condition, String error) {}
    /** 已校验参数类型和公开模板的基础效果步骤。 */
    public record Step(String type, Map<String, RuleExpression.Node> params, String message, Long cashBase) {
        /** 输入领域上下文，解析可选公开文案，不访问白名单外的字段。 */
        public String render(RuleExpression.Context context, long amount) {
            if (message == null) return null;
            return PLACEHOLDER.matcher(message.replace("{effect.amount}", String.valueOf(amount))).replaceAll(match -> java.util.regex.Matcher.quoteReplacement(String.valueOf(RuleExpression.reference(context, match.group(1)))));
        }
    }
    /** 输入合并后的四类配置，静态校验并编译表达式与效果参数。 */
    public static RuleSet compile(RuleDefinition definition) {
        check(definition != null, "规则配置不能为空");
        check(definition.stages != null && !definition.stages.isEmpty() && definition.stages.size() <= 64, "阶段数量不正确");
        check(definition.cards != null && !definition.cards.isEmpty() && definition.cards.size() <= 64, "道具定义数量不正确");
        List<Stage> stages = new ArrayList<>(); Set<String> ids = new HashSet<>();
        for (var stage : definition.stages) {
            check(stage != null && stage.id != null && stage.id.matches("[a-zA-Z0-9_-]{1,80}") && ids.add(stage.id), "阶段 ID 缺失或重复");
            check(Set.of("AUTO_HOOK", "ACTION_WINDOW", "ROLL", "MOVEMENT", "LANDING", "END_TURN").contains(stage.handler == null ? "" : stage.handler), "未知阶段处理器");
            Set<String> actions = stringSet(stage.actions), exit = stringSet(stage.exitOn);
            if (stage.handler.equals("ACTION_WINDOW")) {
                check(Set.of("pre_roll", "manage").contains(stage.phase == null ? "" : stage.phase), "行动窗口需使用兼容的 phase");
                check(!actions.isEmpty() && WINDOW_ACTIONS.containsAll(actions), "行动窗口命令不正确");
                check(!exit.isEmpty() && exit.stream().allMatch(value -> actions.contains(value) || value.equals("replace_roll") && actions.contains("card")), "窗口退出操作不正确");
                check(stage.steps == null || stage.steps.isEmpty(), "行动窗口不能含自动步骤");
            } else check(actions.isEmpty() && exit.isEmpty() && stage.phase == null, "自动阶段不能声明行动窗口");
            if (stage.handler.equals("AUTO_HOOK")) check(stage.hook != null && stage.hook.matches("[A-Z][A-Z0-9_]{0,79}"), "自动阶段 hook 不正确");
            else check(stage.hook == null && (stage.steps == null || stage.steps.isEmpty()), "只有 AUTO_HOOK 可以声明 hook 和步骤");
            stages.add(new Stage(stage.id, stage.handler, stage.hook, stage.phase, actions, exit, steps(stage.steps)));
        }
        validateStages(stages);
        int diceIndex = uniqueHandler(stages, "ROLL");
        for (int index = 0; index < definition.stages.size(); index++) {
            var stage = definition.stages.get(index);
            if (!stage.handler.equals("AUTO_HOOK") || stage.steps == null) continue;
            boolean diceAvailable = index > diceIndex;
            for (var step : stage.steps) {
                if (step.params != null) step.params.values().forEach(value -> validateAutomaticReferences(value, diceAvailable));
                if (step.message != null) { var matcher = PLACEHOLDER.matcher(step.message); while (matcher.find()) validateAutomaticReference(matcher.group(1), diceAvailable); }
            }
        }
        Map<String, CardRule> cards = new LinkedHashMap<>();
        for (var card : definition.cards) {
            check(card != null && card.id != null && card.id.matches("[a-zA-Z0-9_-]{1,80}") && !cards.containsKey(card.id), "卡牌 ID 缺失或重复");
            check(Set.of("ACTIVE", "RENT").contains(card.activation == null ? "" : card.activation), "未知卡牌发动方式");
            check(card.countsAsActive != null, "卡牌必须声明主动次数政策");
            Set<String> windows = stringSet(card.stages); check(!windows.isEmpty(), "卡牌发动阶段不能为空");
            for (String window : windows) {
                if (card.activation.equals("RENT")) check(window.equals("rent_choice") && !card.countsAsActive, "免租政策不正确");
                else check(stages.stream().anyMatch(stage -> stage.id.equals(window) && stage.actions.contains("card")), "卡牌引用了不可出牌的阶段");
            }
            check(card.checks != null && card.checks.size() <= 32, "道具条件列表不正确"); List<Check> checks = new ArrayList<>();
            for (var condition : card.checks) {
                check(condition != null && condition.error != null && !condition.error.isBlank(), "条件提示不能为空");
                var compiled = RuleExpression.compile(condition.condition); RuleExpression.expect(compiled, "BOOLEAN"); checks.add(new Check(compiled, condition.error));
            }
            List<Step> steps = steps(card.steps); check(!steps.isEmpty(), "道具效果不能为空");
            if (card.activation.equals("RENT")) check(steps.getFirst().type.equals("WAIVE_RENT"), "租金防御组合必须首先解决当前费用");
            if (steps.stream().anyMatch(step -> step.type.equals("REPLACE_ROLL"))) {
                check(card.activation.equals("ACTIVE") && windows.stream().allMatch(window -> stages.stream().anyMatch(stage -> stage.id.equals(window) && stage.exitOn.contains("replace_roll"))), "替代骰子必须退出掷骰窗口");
                check(steps.stream().noneMatch(step -> step.type.equals("MOVE") || step.type.equals("SETTLE_TILE")), "替代骰子由阶段统一移动和落点，卡牌不得重复配置");
            }
            cards.put(card.id, new CardRule(card.id, card.activation, windows, card.countsAsActive, List.copyOf(checks), steps));
        }
        check(cards.keySet().containsAll(Arrays.asList(Rules.ITEMS)), "规则包缺少既有六种道具");
        Set<String> eventIds = new HashSet<>();
        List<EventRule> chance = events(definition.chanceEvents, eventIds), fortune = events(definition.fortuneEvents, eventIds);
        List<PassiveRule> passives = new ArrayList<>(); Set<String> passiveIds = new HashSet<>();
        if (definition.passiveRules != null) {
            check(definition.passiveRules.size() <= 32, "被动订阅数量过多");
            for (var passive : definition.passiveRules) {
                check(passive != null && passive.id != null && passive.id.matches("[a-zA-Z0-9_-]{1,80}") && passiveIds.add(passive.id), "被动订阅 ID 缺失或重复");
                check("BUILDINGS_DESTROYED".equals(passive.trigger) && "DISASTER".equals(passive.tag), "未知事实触发或标签");
                check(passive.probability != null && passive.probability >= 0 && passive.probability <= 100 && passive.compensationPercent != null && passive.compensationPercent >= 0 && passive.compensationPercent <= 100, "补偿百分比须在 0～100 之间");
                passives.add(new PassiveRule(passive.id, passive.trigger, passive.tag, passive.probability, passive.compensationPercent));
            }
        }
        return new RuleSet(stages, cards, chance, fortune, passives, definition.content);
    }
    /** 输入事件池和跨池 ID 集合，编译完整等权事件并限定自动上下文，不执行客户端选择。 */
    private static List<EventRule> events(List<RuleDefinition.EventRule> definitions, Set<String> ids) {
        check(definitions != null && !definitions.isEmpty() && definitions.size() <= 32, "事件池不能为空或超过 32 项");
        List<EventRule> events = new ArrayList<>();
        for (var event : definitions) {
            check(event != null && event.id != null && event.id.matches("[a-zA-Z0-9_-]{1,80}") && ids.add(event.id), "事件 ID 缺失或重复");
            check(event.description == null || !event.description.isBlank() && event.description.length() <= 500, "事件说明不可用");
            List<Step> compiled = steps(event.steps); check(!compiled.isEmpty(), "事件效果不能为空");
            check(compiled.stream().allMatch(step -> Set.of("CHARGE_ALL", "ADD_STATUS", "DESTROY_BUILDINGS", "GRANT_MONEY", "DRAW_CARDS", "SKIP_TURN", "PAY_FEE", "MOVE", "SETTLE_TILE", "LOG").contains(step.type)), "事件含不适用的效果");
            for (var step : event.steps) {
                if (step.params != null) step.params.values().forEach(value -> validateAutomaticReferences(value, true));
                if (step.message != null) { var matcher = PLACEHOLDER.matcher(step.message); while (matcher.find()) validateAutomaticReference(matcher.group(1), true); }
            }
            events.add(new EventRule(event.id, event.description, compiled));
        }
        return List.copyOf(events);
    }
    /** 输入阶段序列，确认原协议能到达掷骰、移动、落点和结束窗口。 */
    private static void validateStages(List<Stage> stages) {
        check(stages.getFirst().handler.equals("AUTO_HOOK") && "TURN_START".equals(stages.getFirst().hook), "首阶段必须是 TURN_START");
        int roll = uniqueHandler(stages, "ROLL"), movement = uniqueHandler(stages, "MOVEMENT"), landing = uniqueHandler(stages, "LANDING"), end = uniqueHandler(stages, "END_TURN");
        check(roll < movement && movement < landing && landing < end && end == stages.size() - 1, "回合阶段依赖顺序不正确");
        List<Stage> rollWindows = stages.stream().filter(stage -> stage.actions.contains("roll")).toList();
        List<Stage> endWindows = stages.stream().filter(stage -> stage.actions.contains("endTurn")).toList();
        check(rollWindows.size() == 1 && stages.indexOf(rollWindows.getFirst()) < roll && rollWindows.getFirst().exitOn.contains("roll") && rollWindows.getFirst().phase.equals("pre_roll"), "缺少合法掷骰窗口");
        check(endWindows.size() == 1 && stages.indexOf(endWindows.getFirst()) > landing && endWindows.getFirst().exitOn.contains("endTurn") && endWindows.getFirst().phase.equals("manage"), "缺少合法回合结束窗口");
    }
    /** 输入阶段与处理器名，返回其唯一索引。 */
    private static int uniqueHandler(List<Stage> stages, String handler) {
        List<Integer> indices = java.util.stream.IntStream.range(0, stages.size()).filter(index -> stages.get(index).handler.equals(handler)).boxed().toList();
        check(indices.size() == 1, "阶段处理器需且仅需一次：" + handler); return indices.getFirst();
    }
    /** 输入字符串列表，拒绝空值或重复并返回不可变集合。 */
    private static Set<String> stringSet(List<String> values) {
        if (values == null) return Set.of();
        check(values.stream().allMatch(value -> value != null && !value.isBlank()) && new HashSet<>(values).size() == values.size(), "配置列表含空值或重复"); return Set.copyOf(values);
    }
    /** 输入 DSL 步骤，校验处理器、参数结构、表达式和模板路径。 */
    private static List<Step> steps(List<RuleDefinition.Step> definitions) {
        if (definitions == null) return List.of(); check(definitions.size() <= 64, "组合步骤过多"); List<Step> steps = new ArrayList<>();
        for (var step : definitions) {
            check(step != null && "APPLY_EFFECT".equals(step.op) && EFFECT_PARAMS.containsKey(step.type), "未知步骤或效果处理器");
            Map<String, Object> params = step.params == null ? Map.of() : step.params; Map<String, String> allowed = EFFECT_PARAMS.get(step.type);
            check(allowed.keySet().containsAll(params.keySet()), "效果存在未知参数");
            Set<String> required = switch (step.type) { case "ADD_STATUS" -> Set.of("statusId", "amount"); case "CHARGE_ALL", "GRANT_MONEY", "DRAW_CARDS", "SKIP_TURN", "PAY_FEE", "REPLACE_ROLL", "MOVE" -> Set.of("amount"); case "PLACE_BARRIER", "DEMOLISH" -> Set.of("tileId"); case "SWAP_POSITION" -> Set.of("targetId"); default -> Set.of(); };
            check(params.keySet().containsAll(required), "效果缺少必需参数"); Map<String, RuleExpression.Node> compiled = new LinkedHashMap<>();
            params.forEach((name, value) -> { var node = RuleExpression.compile(value); RuleExpression.expect(node, allowed.get(name)); if (value instanceof Number) RuleExpression.integer(value); compiled.put(name, node); });
            if (step.type.equals("ADD_STATUS")) check("TRAP_IMMUNITY".equals(params.get("statusId")) && params.get("amount") instanceof Number && RuleExpression.integer(params.get("amount")) == 3, "首期状态必须是三个后续个人回合的陷阱免疫");
            if (Set.of("CHARGE_ALL", "GRANT_MONEY", "PAY_FEE").contains(step.type) && params.get("amount") instanceof Number) check(RuleExpression.integer(params.get("amount")) >= 0, "费用或奖励不能为负");
            if (Set.of("DRAW_CARDS", "SKIP_TURN").contains(step.type) && params.get("amount") instanceof Number) { long value = RuleExpression.integer(params.get("amount")); check(value >= 0 && value <= 32, "抽卡或休息次数须在 0～32 之间"); }
            if (step.type.equals("MOVE") && params.get("amount") instanceof Number) { long value = RuleExpression.integer(params.get("amount")); check(value >= -40 && value <= 40, "规则移动步数不可用"); }
            if (step.type.equals("REPLACE_ROLL") && params.get("amount") instanceof Number) { long value = RuleExpression.integer(params.get("amount")); check(value >= 0 && value <= 6, "规则骰子点数不可用"); }
            if (step.type.equals("DESTROY_BUILDINGS")) {
                check(params.containsKey("tileId") != params.containsKey("selector"), "灾害必须且只能声明指定地块或随机选择器");
                if (params.containsKey("selector")) check("RANDOM_BUILT_PROPERTY".equals(params.get("selector")), "未知灾害目标选择器");
            }
            if (step.type.equals("LOG")) check(step.message != null && !step.message.isBlank(), "LOG 需要消息模板");
            if (step.message != null) {
                check(step.message.length() <= 500, "效果消息过长"); var matcher = PLACEHOLDER.matcher(step.message);
                while (matcher.find()) { if (matcher.group(1).equals("effect.amount")) check(Set.of("PAY_FEE", "CHARGE_ALL", "GRANT_MONEY").contains(step.type), "金额文案只用于现金效果"); else RuleExpression.referenceType(matcher.group(1)); }
                check(PLACEHOLDER.matcher(step.message).replaceAll("").indexOf('{') < 0 && PLACEHOLDER.matcher(step.message).replaceAll("").indexOf('}') < 0, "消息模板占位符不正确");
            }
            steps.add(new Step(step.type, Map.copyOf(compiled), step.message, Set.of("PAY_FEE", "CHARGE_ALL", "GRANT_MONEY").contains(step.type) && params.get("amount") instanceof Number ? RuleExpression.integer(params.get("amount")) : null));
        }
        return List.copyOf(steps);
    }
    /** 输入自动阶段参数和骰子可用性，拒绝依赖尚未产生的玩家选择。 */
    private static void validateAutomaticReferences(Object expression, boolean diceAvailable) {
        if (expression instanceof Map<?, ?> map) {
            if (map.containsKey("ref")) validateAutomaticReference(String.valueOf(map.get("ref")), diceAvailable);
            else map.values().forEach(value -> validateAutomaticReferences(value, diceAvailable));
        } else if (expression instanceof List<?> list) list.forEach(value -> validateAutomaticReferences(value, diceAvailable));
    }
    /** 输入白名单路径，限定自动阶段只有行动者和已确定的回合值。 */
    private static void validateAutomaticReference(String ref, boolean diceAvailable) {
        if (ref.equals("effect.amount")) return;
        check(ref.startsWith("actor.") || ref.startsWith("turn.") && (!ref.equals("turn.dice") || diceAvailable), "自动阶段不能引用未产生的输入：" + ref);
    }
}
