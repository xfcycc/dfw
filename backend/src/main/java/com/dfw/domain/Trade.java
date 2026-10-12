package com.dfw.domain;

import java.util.*;
import lombok.Getter;
import lombok.Setter;

/** 交易双方可见的报价。 */
@Getter
@Setter
public class Trade {
    /** 报价标识。 */
    public String id;
    /** 发起人。 */
    public String fromId;
    /** 接收人。 */
    public String toId;
    /** 发起人支付现金。 */
    public long giveCash;
    /** 接收人支付现金。 */
    public long wantCash;
    /** 发起人提供地产。 */
    public List<Integer> giveTiles = new ArrayList<>();
    /** 接收人提供地产。 */
    public List<Integer> wantTiles = new ArrayList<>();
    /** 剩余等待毫秒数。 */
    public long remainingMs;
    /** 在线计时起点，暂停时为空。 */
    public Long runningSince;
}
