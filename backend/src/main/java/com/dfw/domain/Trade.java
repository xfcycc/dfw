package com.dfw.domain;

import java.util.*;
import lombok.Getter;
import lombok.Setter;

/** 交易双方操作的报价；双方及资产明细同步进入公开记录。 */
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
    /** 关联的待支付费用，普通交易为空。 */
    public String debtId;
    /** 双方约定用发起人地产抵扣的债务金额，不发生对应现金转移。 */
    public long debtCredit;
    /** 旧报价计时兼容字段；玩法不计时，固定为零。 */
    public long remainingMs;
    /** 旧报价计时兼容字段；不再计时，固定为空。 */
    public Long runningSince;
}
