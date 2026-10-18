package com.dfw.domain;

import lombok.Getter;
import lombok.Setter;

/** 私人手牌。 */
@Getter
@Setter
public class Card extends ContentResult {
    /** 发现的固定候选，取消后仍保留，避免反复打开重抽；仅持有人可见。 */
    public java.util.List<Card> discoverCandidates = new java.util.ArrayList<>();
}
