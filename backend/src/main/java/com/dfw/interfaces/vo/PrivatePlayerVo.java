package com.dfw.interfaces.vo;

import java.util.*;
import lombok.Getter;
import lombok.Setter;
import com.dfw.domain.*;

/** 仅发给对应玩家的私人状态。 */
@Getter
@Setter
public class PrivatePlayerVo {
    /** 十万年魂环剩余免租次数；零表示未持有或已到期。 */
    public int soulRingRemaining;
    /** 魂环剩余本人回合数，包含当前本人回合。 */
    public int soulRingTurns;
    /** 卍解本回合是否已经发动，耗尽后仍为真。 */
    public boolean bankaiActive;
    /** 卍解统一后续出牌剩余次数。 */
    public int bankaiRemaining;
    /** 本回合可否释放千本樱。 */
    public boolean petalsAvailable;
    /** 本人剩余共享花瓣。 */
    public int petalsRemaining;
    /** 本回合是否可以打开七五折远程施工选择。 */
    public boolean remoteBuildAvailable;
    /** 本人活动选择，仅本人可见，未提交为空。 */
    public Integer activitySelection;
    /** 私人选择对应的活动身份。 */
    public String activityId;
    /** 私人选择对应的输入轮次。 */
    public Integer activityRound;

    /** 固定存档码，观战投影没有此字段。 */
    public String archiveCode;
    /** 仅房主可见的待批准角色申请。 */
    public List<SeatClaimVo> seatClaims = new ArrayList<>();
    /** 上一回合是否存在可投影的主动牌，含UR，不含投影魔术。 */
    public boolean projectionAvailable;
    /** 当前不能使用的本人手牌身份，防御与主动时机各按原规则判断。 */
    public List<String> disabledCardIds = new ArrayList<>();
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
