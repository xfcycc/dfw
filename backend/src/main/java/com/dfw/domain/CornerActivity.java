package com.dfw.domain;

import java.util.*;
import lombok.Getter;
import lombok.Setter;

/** 独立活动实例；内部随机结果与秘密出手不得直接公开。 */
@Getter
@Setter
public class CornerActivity {
    /** 永不复用的活动身份。 */
    public String id;
    /** 原五类玩法或 treasure、bridge、minority、blindbox 趣味变体。 */
    public String kind;
    /** 实际触发地块。 */
    public int tileId;
    /** 踩中地块的玩家。 */
    public String triggerId;
    /** 结束后恢复的父操作阶段。 */
    public String returnPhase;
    /** 选择或展示阶段。 */
    public String stage = "select";
    /** 输入轮次，阶段变化时递增，拒绝过期操作。 */
    public int round = 1;
    /** 固定参与者，包含休息或监禁玩家。 */
    public List<String> participants = new ArrayList<>();
    /** 猜拳仍在场的玩家。 */
    public List<String> contenders = new ArrayList<>();
    /** 已提交的选择，猜拳揭晓前只对本人公开。 */
    public Map<String, Integer> choices = new LinkedHashMap<>();
    /** 红包领取者与选中序号，一至参与人数；旧活动为空。 */
    public Map<String, Integer> packetSelections = new LinkedHashMap<>();
    /** 本场已经扣除的自愿下注。 */
    public Map<String, Long> wagers = new LinkedHashMap<>();
    /** 按参与者顺序索引的隐藏红包基础金额，领取前不公开。 */
    public Map<String, Long> packets = new LinkedHashMap<>();
    /** 同一红包索引对应的幸运加奖，领取前不公开。 */
    public Map<String, Long> bonuses = new LinkedHashMap<>();
    /** 已实际发放的总到账金额。 */
    public Map<String, Long> payouts = new LinkedHashMap<>();
    /** 上一轮已公开的手势。 */
    public Map<String, Integer> revealed = new LinkedHashMap<>();
    /** 骰子或赛马的服务端随机结果。 */
    public int sample;
    /** 宝箱已通过层数或玻璃桥当前关数，初始为零。 */
    public int level;
    /** 已揭晓的逐层安全方向，未揭晓方向不公开。 */
    public List<Integer> safePaths = new ArrayList<>();
    /** 二十个盲盒的隐藏款序号，仅服务端保存。 */
    public int hiddenBox;
    /** 本场隐藏款是否升级限定款，仅领取隐藏款时揭晓。 */
    public boolean limitedBox;
    /** 内部子弹位置，范围零到五。 */
    public int bullet;
    /** 已经开枪的次数。 */
    public int shots;
    /** 指定对手。 */
    public String opponentId;
    /** 当前需要开枪的玩家。 */
    public String currentPlayerId;
    /** 最近一次开枪的玩家。 */
    public String lastShooterId;
    /** 最近一次是否命中，揭晓前不公开。 */
    public boolean hit;
    /** 活动奖励已全部结算或已产生债务，不能退款重结。 */
    public boolean settled;
    /** 当前展示阶段服务端开始时间。 */
    public long startedAt;
    /** 自动展示阶段结束时间；输入阶段为空。 */
    public Long endsAt;
    /** 可公开的结果说明。 */
    public String summary = "";
}
