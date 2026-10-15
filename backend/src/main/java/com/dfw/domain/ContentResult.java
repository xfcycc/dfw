package com.dfw.domain;

import java.util.*;
import lombok.Getter;
import lombok.Setter;

/** 已抽取并冻结的内容结果；公开事件与私人卡牌共用卡面，参数为本局实际值。 */
@Getter
@Setter
public class ContentResult {
    /** 道具类型；事件结果为空，私人卡面恢复时保留发动方式。 */
    public String kind;
    /** 内容结果身份。 */
    public String id;
    /** chance、fortune 或 card。 */
    public String pool;
    /** 已选机制。 */
    public String mechanism;
    /** 已选完整参数组合标识。 */
    public String parameterId;
    /** 池前缀限定的皮肤标识。 */
    public String skinId;
    /** 卡面名称。 */
    public String name;
    /** 抽取时冻结的实际影响等级：R、SR、SSR 或 UR。 */
    public String rarity;
    /** 素材卡牌类型：主动、防御、持续或即时；具体发动时机另行显示。 */
    public String cardType;
    /** 原创题材台词。 */
    public String quote;
    /** 场景描述。 */
    public String description;
    /** 根据实际参数生成的规则文案。 */
    public String effect;
    /** 固定后的数值参数，不在恢复时重新换算金额。 */
    public Map<String, Long> parameters = new LinkedHashMap<>();
    /** 已发生的赌博判定样本，提交后保存，恢复不重抽。 */
    public Integer sample;
    /** 排名事件在抽取时锁定的唯一目标。 */
    public String targetId;
    /** 是否只允许获得者看到整张卡面。 */
    public boolean privateSupply;
}
