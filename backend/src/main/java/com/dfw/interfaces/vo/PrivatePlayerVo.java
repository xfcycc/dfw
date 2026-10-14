package com.dfw.interfaces.vo;

import java.util.*;
import lombok.Getter;
import lombok.Setter;
import com.dfw.domain.*;

/** 仅发给对应玩家的私人状态。 */
@Getter
@Setter
public class PrivatePlayerVo {
    /** 固定存档码，观战投影没有此字段。 */
    public String archiveCode;
    /** 仅房主可见的待批准角色申请。 */
    public List<SeatClaimVo> seatClaims = new ArrayList<>();
    /** 本人的完整内容选择。 */
    public ContentChoiceVo choice;
    /** 本人获得的私人事件卡面。 */
    public ContentResult event;
    /** 本人最近一次补给或被转移的卡面。 */
    public ContentResult notice;
    /** 私人结果说明。 */
    public String noticeText;
    /** 本人标识。 */
    public String playerId;
    /** 本人手牌。 */
    public List<Card> hand;
    /** 仅交易双方可见的报价。 */
    public Trade trade;
    /** 本人发起的最近报价被拒绝时的说明，仅发起人可见。 */
    public String tradeRejection;
    /** 本人是否已主动使用道具。 */
    public boolean activeCardUsed;
}
