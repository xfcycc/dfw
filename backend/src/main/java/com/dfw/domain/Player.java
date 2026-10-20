package com.dfw.domain;

import java.util.*;
import lombok.Getter;
import lombok.Setter;

/** 内部玩家状态。 */
@Getter
@Setter
public class Player {
    /** 随对局保存的统计及冻结结算；旧存档可为空。 */
    public MatchStats matchStats;
    /** 机器人难度；空值表示真人，easy、normal、hard。 */
    public String botDifficulty;
    /** 最近主动报价的全局回合编号，用于限制跨回合报价频率。 */
    public int botTradeTurn = -1;
    /** 最近三十二份主动报价的条件与回合，随存档保存，避免跨回合重报相同条件。 */
    public Map<String, Integer> botTradeQuotes = new LinkedHashMap<>();
    /** 最新私人卡面（补给或双方可见的被转移牌）。 */
    public ContentResult contentNotice;
    /** 私人结果说明，不发给观战者。 */
    public String contentNoticeText;
    /** 本人发起的最近报价被拒绝时的说明，新报价成功发出后清除。 */
    public String tradeRejection;
    /** 玩家唯一标识。 */
    public String id;
    /** 公开昵称。 */
    public String name;
    /** 玩家选择的公开形象，旧快照缺省使用探险家。 */
    public String avatarId = "explorer";
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
    /** 已开始的个人回合编号，包含被休息跳过的回合。 */
    public int personalTurns;
    /** 最近一次个人回合开始时的位置。 */
    public int turnStartPosition;
    /** 本局永久扣减的手牌位，旧快照为零，新局重置。 */
    public int handLimitReduction;
}
