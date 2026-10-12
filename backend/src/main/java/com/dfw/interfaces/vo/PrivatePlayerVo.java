package com.dfw.interfaces.vo;

import java.util.*;
import lombok.Getter;
import lombok.Setter;
import com.dfw.domain.*;

/** 仅发给对应玩家的私人状态。 */
@Getter
@Setter
public class PrivatePlayerVo {
    /** 本人标识。 */
    public String playerId;
    /** 本人手牌。 */
    public List<Card> hand;
    /** 仅交易双方可见的报价。 */
    public Trade trade;
    /** 本人是否已主动使用道具。 */
    public boolean activeCardUsed;
}
