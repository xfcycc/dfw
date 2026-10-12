package com.dfw.domain.rules;

import com.dfw.domain.*;
import java.util.Map;

/** 启动时固定的版本目录，旧版 classic-v1 保持原 Java 路径。 */
public final class RuleCatalog {
    private final String activeVersion;
    private final Map<String, RuleSet> versions;
    /** 输入当前新局版本和全部编译包，保存不可变版本目录。 */
    public RuleCatalog(String activeVersion, Map<String, RuleSet> versions) {
        RuleExpression.check(versions.containsKey(activeVersion), "新局规则版本未加载：" + activeVersion);
        this.activeVersion = activeVersion; this.versions = Map.copyOf(versions);
    }
    /** 返回仅用于新开对局的规则包。 */
    public RuleSet active() { return versions.get(activeVersion); }
    /** 输入房间，返回是否使用配置阶段和 DSL。 */
    public boolean configured(Room room) { return room.runtime != null && !"classic-v1".equals(room.runtime.ruleSetVersion); }
    /** 输入房间，返回绑定版本并核对文件摘要，缺失或变更时拒绝继续。 */
    public RuleSet bound(Room room) {
        RuleSet rules = versions.get(room.runtime.ruleSetVersion);
        if (rules == null || !rules.getDigest().equals(room.runtime.ruleSetDigest)) throw new IllegalStateException("房间 " + room.code + " 的规则版本缺失或内容变更：" + room.runtime.ruleSetVersion);
        return rules;
    }
}
