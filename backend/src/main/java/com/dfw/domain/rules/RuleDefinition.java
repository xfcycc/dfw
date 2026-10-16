package com.dfw.domain.rules;

import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

/** 启动时加载的规则 JSON 结构，编译后不进入房间快照。 */
@Getter
@Setter
public class RuleDefinition {
    /** 已确认的分层内容目录。 */
    public ContentCatalog content;
    /** 按顺序推进的回合阶段。 */
    public List<Stage> stages;
    /** 全部等权机会事件。 */
    public List<EventRule> chanceEvents;
    /** 全部等权财富事件。 */
    public List<EventRule> fortuneEvents;
    /** 实际灾害损失的补偿订阅。 */
    public List<PassiveRule> passiveRules;
    /** 按现有卡牌 kind 标识的定义。 */
    public List<CardRule> cards;

    /** 机会文件独立根结构，包含实际损失订阅。 */
    @Getter
    @Setter
    public static class ChanceFile {
        /** 机制、参数与匹配皮肤。 */
        public ContentDefinition content;
        /** 全部机会事件。 */
        public List<EventRule> events;
        /** 灾害损失补偿订阅。 */
        public List<PassiveRule> passiveRules;
    }
    /** 财富文件独立根结构。 */
    @Getter
    @Setter
    public static class FortuneFile {
        /** 机制、参数与匹配皮肤。 */
        public ContentDefinition content;
        /** 全部财富事件。 */
        public List<EventRule> events;
    }
    /** 卡牌文件独立根结构。 */
    @Getter
    @Setter
    public static class CardFile {
        /** 机制、参数与匹配皮肤。 */
        public ContentDefinition content;
        /** 六种卡牌定义。 */
        public List<CardRule> cards;
    }
    /** 公共回合文件独立根结构。 */
    @Getter
    @Setter
    public static class TurnFile {
        /** 顺序回合阶段。 */
        public List<Stage> stages;
    }

    /** 首期事实订阅，仅支持灾害建筑损失后按业主独立判定补偿。 */
    @Getter
    @Setter
    public static class PassiveRule {
        /** 类别内唯一订阅标识。 */
        public String id;
        /** 实际结果触发类型，当前为 BUILDINGS_DESTROYED。 */
        public String trigger;
        /** 匹配的效果标签，当前为 DISASTER。 */
        public String tag;
        /** 每次实际损失事件的成功概率，整数百分比。 */
        public Integer probability;
        /** 按摧毁前租金快照补偿的整数百分比。 */
        public Integer compensationPercent;
    }

    /** 机会或财富事件定义，不增加前端命令或私人卡牌类型。 */
    @Getter
    @Setter
    public static class EventRule {
        /** 两类事件池之间唯一的业务标识。 */
        public String id;
        /** 可选公开扩展玩法说明，缺省不进入追加说明。 */
        public String description;
        /** 自动执行的基础效果组合。 */
        public List<Step> steps;
    }

    /** 自动阶段或兼容前端的行动窗口。 */
    @Getter
    @Setter
    public static class Stage {
        /** 阶段标识。 */
        public String id;
        /** 注册的阶段处理器。 */
        public String handler;
        /** 自动触发点。 */
        public String hook;
        /** 行动窗口向前端投影的阶段。 */
        public String phase;
        /** 允许的原协议命令。 */
        public List<String> actions;
        /** 使窗口退出的命令或 replace_roll 结果。 */
        public List<String> exitOn;
        /** 进入自动阶段时执行的组合。 */
        public List<Step> steps;
    }

    /** 道具的发动政策、条件和效果组合。 */
    @Getter
    @Setter
    public static class CardRule {
        /** 现有卡牌种类标识。 */
        public String id;
        /** ACTIVE 主动发动或 RENT 租金选择。 */
        public String activation;
        /** 允许的阶段标识，租金选择使用 rent_choice。 */
        public List<String> stages;
        /** 是否占用本回合主动道具次数。 */
        public Boolean countsAsActive;
        /** 按顺序校验的发动条件。 */
        public List<Check> checks;
        /** 校验通过并消耗后执行的组合。 */
        public List<Step> steps;
    }

    /** 带兼容业务提示的条件。 */
    @Getter
    @Setter
    public static class Check {
        /** 受控 AST 条件。 */
        public Object condition;
        /** 条件不成立的原业务提示。 */
        public String error;
    }

    /** 最小 DSL 的基础效果调用。 */
    @Getter
    @Setter
    public static class Step {
        /** 当前支持 APPLY_EFFECT。 */
        public String op;
        /** 已注册的效果处理器类型。 */
        public String type;
        /** 参数常量或白名单引用表达式。 */
        public Map<String, Object> params;
        /** 可选公开文案模板，只允许引用白名单字段。 */
        public String message;
    }
}
