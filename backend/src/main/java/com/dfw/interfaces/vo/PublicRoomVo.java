package com.dfw.interfaces.vo;

import java.util.*;
import lombok.Getter;
import lombok.Setter;
import com.dfw.domain.*;

/** 公开房间白名单。 */
@Getter
@Setter
public class PublicRoomVo {
    /** 本局公开经济参数，旧对局缺省按一倍基准。 */
    public EconomyRules economy;
    /** 本局新增机会结果的公开说明，空列表表示原事件池。 */
    public List<String> chanceEventDescriptions = new ArrayList<>();
    /** 六位房间号。 */
    public String code;
    /** 已提交状态版本。 */
    public long version = 1;
    /** 房间阶段。 */
    public String status = "lobby";
    /** 公开地图大小：small、medium、large，旧房间缺省为 large。 */
    public String mapSize;
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
    /** 公开玩家信息。 */
    public List<PublicPlayerVo> players = new ArrayList<>();
    /** 是否存在待确认报价。 */
    public boolean tradePending;
}
