package com.dfw.interfaces.vo;

import java.util.*;
import lombok.Getter;
import lombok.Setter;
import com.dfw.domain.*;

/** 仅向实际选择者投影的内容选项，不含父队列、流程或其他玩家手牌。 */
@Getter
@Setter
public class ContentChoiceVo {
    /** 本次选择身份。 */
    public String id;
    /** 选择类型。 */
    public String type;
    /** 合法地产候选。 */
    public List<Integer> tileIds;
    /** 仅超限整理时提供本人全部候选牌。 */
    public List<Card> cards;
    /** 发动免费升级的未消费卡。 */
    public String cardId;
    /** 相关已确定卡面。 */
    public ContentResult content;
    /** 施加的跳过次数。 */
    public int skipTurns;
    /** 是否监禁限制。 */
    public boolean jailed;
    /** 每张弃牌的本局实际折现金额。 */
    public long discardCash;
}
