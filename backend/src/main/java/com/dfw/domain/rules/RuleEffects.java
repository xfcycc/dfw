package com.dfw.domain.rules;

import com.dfw.domain.*;
import java.util.*;
import static com.dfw.domain.GameError.require;

/** 阶段、道具及机会/财富事件共用的效果参数解析。 */
public final class RuleEffects {
    /** 输入组合步骤和上下文，解析整数、目标及模板，返回可持久化的具体效果。 */
    public static List<GameEffect> plan(List<RuleSet.Step> steps, RuleExpression.Context context, EffectResolver effects) {
        return plan(steps, context, effects, "stage");
    }
    /** 输入步骤、上下文及来源标识，仅对现金效果应用本局倍率或固定金额，恢复效果不重复换算。 */
    public static List<GameEffect> plan(List<RuleSet.Step> steps, RuleExpression.Context context, EffectResolver effects, String scope) {
        List<GameEffect> result = new ArrayList<>(); int stepIndex = 0;
        for (var step : steps) {
            GameEffect effect = effects.effect(step.type(), context.actor());
            for (var entry : step.params().entrySet()) {
                Object value = entry.getValue().value(context);
                switch (entry.getKey()) {
                    case "amount" -> effect.amount = RuleExpression.integer(value);
                    case "tileId" -> { long id = RuleExpression.integer(value); require(id >= 0 && id < context.room().board.size(), "请选择有效地产"); effect.tileId = (int) id; }
                    case "targetId" -> { require(value instanceof String, "请选择有效目标玩家"); effect.targetId = (String) value; }
                    case "statusId" -> effect.statusId = (String) value;
                    case "selector" -> effect.selector = (String) value;
                    default -> throw new IllegalStateException("未校验的效果参数");
                }
            }
            if (Set.of("PAY_FEE", "CHARGE_ALL", "GRANT_MONEY").contains(step.type())) effect.amount = step.cashBase() != null ? Rules.economy(context.room()).money(scope + "." + stepIndex + ".amount", step.cashBase()) : Rules.economy(context.room()).derived(scope + "." + stepIndex + ".amount", effect.amount);
            if (step.type().equals("MOVE")) require(effect.amount >= -40 && effect.amount <= 40, "规则移动步数不可用");
            if (Set.of("PAY_FEE", "CHARGE_ALL", "GRANT_MONEY").contains(step.type())) require(effect.amount >= 0, "规则费用或奖励不可用");
            if (Set.of("DRAW_CARDS", "SKIP_TURN").contains(step.type())) require(effect.amount >= 0 && effect.amount <= 32, "抽卡或休息次数不可用");
            if (step.type().equals("ADD_STATUS")) require("TRAP_IMMUNITY".equals(effect.statusId) && effect.amount == 3, "持续状态参数不可用");
            effect.message = step.render(context, effect.amount); result.add(effect); stepIndex++;
        }
        return result;
    }
}
