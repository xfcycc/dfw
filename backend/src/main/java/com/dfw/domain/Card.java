package com.dfw.domain;

import lombok.Getter;
import lombok.Setter;

/** 私人手牌。 */
@Getter
@Setter
public class Card extends ContentResult {
    /** 临时投影牌到期的全局回合，零为永久牌。 */
    public int expiresAtTurn;
    /** 本回合投影专属额外使用资格。 */
    public boolean projected;
    /** 剑雨剩余次数，零表示新取得时的两次。 */
    public int remainingUses;
    /** 剑雨最近使用的全局回合，零表示尚未使用。 */
    public int lastUsedTurn;
    /** 重塑继承的已消耗次数，换成次数牌也不能刷新。 */
    public int usesSpent;
    /** 融合已经固定但尚未确认的结果及有序材料。 */
    public Card fusionResult;
    /** 固定融合结果对应的第一张、第二张材料身份。 */
    public java.util.List<String> fusionMaterials = new java.util.ArrayList<>();
    /** 发现的固定候选，取消后仍保留，避免反复打开重抽；仅持有人可见。 */
    public java.util.List<Card> discoverCandidates = new java.util.ArrayList<>();
}
