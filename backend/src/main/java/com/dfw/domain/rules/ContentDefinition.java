package com.dfw.domain.rules;

import java.util.*;
import lombok.Getter;
import lombok.Setter;

/** 内容文件；道具在稀有度内均分完整效果，事件独立配权，皮肤只改变表达。 */
@Getter
@Setter
public class ContentDefinition {
    /** 池中全部机制，包括显式停用的素材。 */
    public List<Mechanism> mechanisms;
    /** 机制启用状态及其参数档。 */
    @Getter @Setter
    public static class Mechanism {
        /** 已支持的机制标识。 */
        public String id;
        /** 是否进入抽取；停用素材仍保留配置。 */
        public boolean enabled = true;
        /** 参数组织分组，当前统一使用 default。 */
        public List<Group> groups;
    }
    /** 完整参数的组织分组，不参与概率计算。 */
    @Getter @Setter
    public static class Group {
        /** 档标识。 */
        public String id;
        /** 去重后的完整参数组合。 */
        public List<Parameter> parameters;
    }
    /** 一个确定参数组合及匹配皮肤。 */
    @Getter @Setter
    public static class Parameter {
        /** 机制内稳定身份，也用于固定金额键。 */
        public String id;
        /** 实际效果的影响等级：R、SR、SSR 或 UR，同效果卡面必须一致。 */
        public String rarity;
        /** 事件完整效果的抽取权重；道具统一为1，由执行器按档位及效果均分。 */
        public Integer weight;
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
        /** 与所属完整效果一致的 R、SR、SSR 或 UR。 */
        public String rarity;
        /** 原创台词。 */
        public String quote;
        /** 原创场景描述。 */
        public String description;
    }
}
