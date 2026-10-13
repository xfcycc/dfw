package com.dfw.domain;

import java.util.*;
import lombok.Getter;
import lombok.Setter;

/** 持久化房间聚合；不得直接推送到客户端。 */
@Getter
@Setter
public class Room {
    /** 开局固定的经济参数；旧对局缺失时沿用一倍基准。 */
    public EconomyRules economy;
    /** 本局版本的公开追加机会玩法，旧快照缺失时为空。 */
    public List<String> chanceEventDescriptions = new ArrayList<>();
    /** 通用结算运行时；旧快照缺失时继续使用原引擎。 */
    public GameRuntime runtime;
    /** 六位房间号。 */
    public String code;
    /** 已提交状态版本。 */
    public long version = 1;
    /** 房间阶段。 */
    public String status = "lobby";
    /** 房主开局前选择的地图大小，缺省为大地图，再开沿用。 */
    public String mapSize = "large";
    /** 房主标识。 */
    public String hostId;
    /** 本局标识。 */
    public String gameId;
    /** 本局固定棋盘。 */
    public List<Tile> board = new ArrayList<>();
    /** 行动顺序。 */
    public List<String> turnOrder = new ArrayList<>();
    /** 当前顺序索引。 */
    public int turnIndex;
    /** 回合计数。 */
    public int turnNumber;
    /** 当前操作阶段。 */
    public String phase = "pre_roll";
    /** 公开待处理事项。 */
    public PendingDecision pending;
    /** 暂停原因。 */
    public String pausedReason;
    /** 本次骰子结果。 */
    public Integer dice;
    /** 最新移动路径。 */
    public Motion motion;
    /** 胜者标识。 */
    public String winnerId;
    /** 公开事件记录。 */
    public List<PublicLog> logs = new ArrayList<>();
    /** 内部玩家状态。 */
    public List<Player> players = new ArrayList<>();
    /** 仅用于地图的种子。 */
    public String seed = "";
    /** 私人待确认报价。 */
    public Trade trade;
    /** 本回合是否已主动使用道具。 */
    public boolean activeCardUsed;
    /** 本回合是否已抽取事件。 */
    public boolean eventDrawn;
    /** 本回合已升级地产。 */
    public List<Integer> upgradedTiles = new ArrayList<>();
}
