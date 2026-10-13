package com.dfw.domain.rules;

import com.dfw.domain.*;
import java.util.*;
import static com.dfw.domain.GameError.require;

/** 阶段、道具及机会事件共用的效果参数解析。 */
public final class RuleEffects {
    /** 输入组合步骤和上下文，解析整数、目标及模板，返回可持久化的具体效果。 */
    public static List<GameEffect> plan(List<RuleSet.Step> steps, RuleExpression.Context context, EffectResolver effects) {
        List<GameEffect> result = new ArrayList<>();
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
                    case "probability" -> effect.probability = percent(value);
                    case "compensationPercent" -> effect.compensationPercent = percent(value);
                    default -> throw new IllegalStateException("未校验的效果参数");
                }
            }
            if (step.type().equals("MOVE")) require(effect.amount >= -40 && effect.amount <= 40, "规则移动步数不可用");
            if (Set.of("PAY_FEE", "CHARGE_ALL").contains(step.type())) require(effect.amount >= 0, "规则费用不可用");
            if (step.type().equals("ADD_STATUS")) require("TRAP_IMMUNITY".equals(effect.statusId) && effect.amount == 3, "持续状态参数不可用");
            effect.message = step.render(context); result.add(effect);
        }
        return result;
    }
    /** 输入百分比表达式值，返回 0～100 的整数，越界拒绝。 */
    private static int percent(Object value) {
        long percent = RuleExpression.integer(value); require(percent >= 0 && percent <= 100, "补偿百分比不可用"); return (int) percent;
    }
}
