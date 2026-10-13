package com.dfw.interfaces.vo;

import lombok.Getter;
import lombok.Setter;

/** 公开玩家白名单，不包含手牌和身份。 */
@Getter
@Setter
public class PublicPlayerVo {
    /** 玩家唯一标识。 */
    public String id;
    /** 公开昵称。 */
    public String name;
    /** 座位颜色。 */
    public String color;
    /** 可用现金。 */
    public long cash;
    /** 扣除固定赎回费用后的公开净资产。 */
    public long netWorth;
    /** 未破产玩家的净资产名次，同额同名次，清算后为空。 */
    public Integer assetRank;
    /** 棋盘位置。 */
    public int position;
    /** 大厅准备状态。 */
    public boolean ready;
    /** 当前连接状态。 */
    public boolean online;
    /** 是否已破产。 */
    public boolean bankrupt;
    /** 待跳过回合数。 */
    public int skipTurns;
    /** 是否服刑。 */
    public boolean jailed;
    /** 陷阱免疫尚未结束的后续个人回合数，零表示没有免疫。 */
    public int trapImmunityTurns;
    /** 淘汰顺序，未淘汰为空。 */
    public Integer eliminationOrder;
}
