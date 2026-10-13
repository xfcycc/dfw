package com.dfw.domain;

import java.util.*;
import lombok.Getter;
import lombok.Setter;

/** 领域操作，不依赖网络协议。 */
@Getter
@Setter
public class Action {
    /** 操作类型。 */
    public String type;
    /** 大厅地图设置，small、medium 或 large。 */
    public String mapSize;
    /** 准备状态。 */
    public Boolean value;
    /** 免租选择。 */
    public Boolean useCard;
    /** 道具标识。 */
    public String cardId;
    /** 点数。 */
    public Integer point;
    /** 地块索引。 */
    public Integer tileId;
    /** 目标玩家。 */
    public String playerId;
    /** 交易接收人。 */
    public String toId;
    /** 提供现金。 */
    public Long giveCash;
    /** 索取现金。 */
    public Long wantCash;
    /** 提供地产。 */
    public List<Integer> giveTiles;
    /** 索取地产。 */
    public List<Integer> wantTiles;
    /** 报价中的协商抵债金额，省略视为普通资产交换。 */
    public Long debtCredit;
    /** 报价回应。 */
    public Boolean accept;
}
