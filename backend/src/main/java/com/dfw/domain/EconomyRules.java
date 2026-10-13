package com.dfw.domain;

import java.util.*;
import lombok.Getter;
import lombok.Setter;
import static com.dfw.domain.GameError.require;

/** 新局经济配置与本局固定快照；缺失配置的旧对局使用一倍基准。 */
@Getter
@Setter
public class EconomyRules {
    /** 固定现金金额的默认倍率，不作用于比例、步数或玩家交易输入。 */
    public double moneyMultiplier = 1;
    /** 本局基础租金百分比；缺失字段的已有快照保留原 10%。 */
    public int rentBasePercent = 10;
    /** 最终金额覆盖：基础项目、有效地价的派生项目及事件步骤。 */
    public Map<String, Long> fixedAmounts = new LinkedHashMap<>();
    /** 输入金额键和基准金额，返回固定覆盖或按倍率四舍五入后的金额。 */
    public long money(String key, long base) {
        return fixedAmounts.getOrDefault(key, Math.round(base * moneyMultiplier));
    }
    /** 输入金额键和已按实际地价计算的金额，返回覆盖值，不重复应用倍率。 */
    public long derived(String key, long actual) { return fixedAmounts.getOrDefault(key, actual); }
    /** 输入无，返回独立的本局金额与租金比例快照，后续配置修改不影响进行中的对局。 */
    public EconomyRules snapshot() {
        EconomyRules value = new EconomyRules(); value.moneyMultiplier = moneyMultiplier; value.rentBasePercent = rentBasePercent; value.fixedAmounts = new LinkedHashMap<>(fixedAmounts); return value;
    }
    /** 输入无，校验倍率、基础租金百分比及金额配置，非法配置抛出业务错误。 */
    public void validate() {
        require(Double.isFinite(moneyMultiplier) && moneyMultiplier > 0 && 6000 * moneyMultiplier <= 9007199254740991L, "经济倍率不可用");
        require(rentBasePercent >= 0 && rentBasePercent <= 100, "基础租金百分比须在 0～100 之间");
        require(fixedAmounts != null && fixedAmounts.entrySet().stream().allMatch(e -> e.getKey() != null && !e.getKey().isBlank() && e.getValue() != null && e.getValue() >= 0 && e.getValue() <= 9007199254740991L), "固定金额配置不可用");
        for (int i = 0; i < Rules.PRICES.length; i++) require(money("landPrice." + i, Rules.PRICES[i]) > 0, "地价必须大于零");
    }
}
