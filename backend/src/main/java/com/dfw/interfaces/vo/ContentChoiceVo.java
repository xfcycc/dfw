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
    /** 仅本人可见的玩家候选。 */
    public List<String> playerIds;
    /** 本次选择提示。 */
    public String prompt;
    /** 合法地产候选。 */
    public List<Integer> tileIds;
    /** 超限、制衡或发现时仅提供选择者本人的候选牌。 */
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
    /** 当前实际手牌上限。 */
    public int handLimit;
}
