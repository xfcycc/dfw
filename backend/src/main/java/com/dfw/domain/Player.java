package com.dfw.domain;

import java.util.*;
import lombok.Getter;
import lombok.Setter;

/** 内部玩家状态。 */
@Getter
@Setter
public class Player {
    /** 玩家唯一标识。 */
    public String id;
    /** 公开昵称。 */
    public String name;
    /** 座位颜色。 */
    public String color;
    /** 可用现金。 */
    public long cash;
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
    /** 淘汰顺序，未淘汰为空。 */
    public Integer eliminationOrder;
    /** 持续状态；旧快照缺失时为空。 */
    public List<PlayerStatus> statuses = new ArrayList<>();
    /** 私人手牌，仅在服务端及本人状态中使用。 */
    public List<Card> hand = new ArrayList<>();
}
