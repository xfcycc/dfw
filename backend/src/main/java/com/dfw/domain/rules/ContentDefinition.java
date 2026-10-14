package com.dfw.domain.rules;

import java.util.*;
import lombok.Getter;
import lombok.Setter;

/** 机制、金额档、完整参数与皮肤的分层内容文件，不将皮肤当成独立玩法。 */
@Getter
@Setter
public class ContentDefinition {
    /** 池中全部机制，包括权重为零的停用素材。 */
    public List<Mechanism> mechanisms;
    /** 机制概率与其参数档。 */
    @Getter @Setter
    public static class Mechanism {
        /** 已支持的机制标识。 */
        public String id;
        /** 整数百分比，池内合计 100。 */
        public int weight;
        /** 参数档；无金额档时只有 default。 */
        public List<Group> groups;
    }
    /** 金额档或单一默认档，档内完整参数等概率。 */
    @Getter @Setter
    public static class Group {
        /** 档标识。 */
        public String id;
        /** 机制内档概率，合计 100。 */
        public int weight;
        /** 去重后的完整参数组合。 */
        public List<Parameter> parameters;
    }
    /** 一个确定参数组合及匹配皮肤。 */
    @Getter @Setter
    public static class Parameter {
        /** 机制内稳定身份，也用于固定金额键。 */
        public String id;
        /** 步数、金额、次数或概率；类型不混用。 */
        public Map<String, Long> values;
        /** 只改变卡面文案的皮肤。 */
        public List<Skin> skins;
    }
    /** 皮肤元数据，不含机制概率或资料出处。 */
    @Getter @Setter
    public static class Skin {
        /** 道具素材类型，事件皮肤为空。 */
        public String cardType;
        /** 池前缀限定的素材身份。 */
        public String id;
        /** 名称。 */
        public String name;
        /** R、SR 或 SSR。 */
        public String rarity;
        /** 原创台词。 */
        public String quote;
        /** 原创场景描述。 */
        public String description;
    }
}
