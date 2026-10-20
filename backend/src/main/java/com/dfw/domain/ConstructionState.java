package com.dfw.domain;

import java.util.*;
import lombok.Getter;
import lombok.Setter;

/** 建设扩展的持久化进度；与房间快照共同提交，不另行写库。 */
@Getter @Setter
public class ConstructionState {
    /** 每名玩家已经结束的个人回合数，包含休息回合。 */
    public Map<String,Integer> endedTurns = new HashMap<>();
    /** 玩家远程施工到期时的个人已结束回合数。 */
    public Map<String,Integer> remoteUntil = new HashMap<>();
    /** 每名玩家最近一次远程施工的全局回合。 */
    public Map<String,Integer> remoteUsedTurn = new HashMap<>();
    /** 具有两次成功额度的地产连接。 */
    public List<Link> links = new ArrayList<>();
    /** 奉纳标记，键为地产编号，产权变化时失效。 */
    public Map<Integer,Shrine> shrines = new HashMap<>();
    /** 当前多人建造事件的固定顺序，已选择者移除。 */
    public List<BuildTask> tasks = new ArrayList<>();
    /** 同一黄金树事件已经成功建造的地块，供同人连续升级。 */
    public Set<Integer> eventBuiltTiles = new HashSet<>();

    /** 连接两处地产的有限建造奖励。 */
    @Getter @Setter
    public static class Link {
        /** 创建连接的业主。 */
        public String ownerId;
        /** 第一处地产。 */
        public int first;
        /** 第二处地产。 */
        public int second;
        /** 剩余成功次数。 */
        public int charges = 2;
        /** 到期时业主已结束的个人回合数。 */
        public int untilTurn;
    }

    /** 其他玩家完成普通落点结算后生长的标记。 */
    @Getter @Setter
    public static class Shrine {
        /** 施加标记时的业主。 */
        public String ownerId;
        /** 尚可成功生长的次数。 */
        public int charges = 2;
    }

    /** 多人事件中的一份独立可放弃升级奖励。 */
    @Getter @Setter
    public static class BuildTask {
        /** 奖励接收者，在事件开始时锁定。 */
        public String playerId;
        /** 事件完整卡面。 */
        public ContentResult source;
        /** 是否允许在同事件已升级地产继续升级。 */
        public boolean allowRepeat;
    }
}
