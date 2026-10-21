package com.dfw.domain;

import lombok.Getter;
import lombok.Setter;

/** 棋盘地块。 */
@Getter
@Setter
public class Tile {
    /** 格子索引。 */
    public int id;
    /** 地块类型。 */
    public String type = "property";
    /** 活动角当前玩法，普通地块为空。 */
    public String activityKind;
    /** 地标名称。 */
    public String name = "";
    /** 街区编号。 */
    public int district;
    /** 地区稳定标识，目前只用于地名元数据。 */
    public String regionId;
    /** 本局地区展示名，旧地图缺失时沿用街区名。 */
    public String regionName;
    /** 地价。 */
    public long price;
    /** 所有者，空为无主。 */
    public String ownerId;
    /** 建筑等级。 */
    public int level;
    /** 是否抵押。 */
    public boolean mortgaged;
    /** 抵押时土地加建筑估值；旧土地抵押缺省按地价计算。 */
    public Long mortgageBase;
    /** 是否有路障。 */
    public boolean barrier;
    /** 是否存在建筑拆除护盾，随地产保存和转移；保留旧快照字段。 */
    public boolean demolitionShield;
    /** 防拆护盾剩余层数；旧快照仅有布尔标记时按一层处理。 */
    public int demolitionShieldCharges;
    /** 首位正常落点玩家可领取的宝藏标记。 */
    public boolean treasure;
    /** 下一次正租金的演唱会层数，每层乘以 1.2。 */
    public int concertStacks;
    /** 独立于普通路障的剩余陨星截停次数。 */
    public int meteorCharges;
    /** 猩红腐败剩余触发次数，随地产保存。 */
    public int corruptionTurns;
    /** 恩佐斯污染绑定的业主，产权变化立即失效。 */
    public String nzothOwnerId;
    /** 零域施术者，空表示未冰封。 */
    public String zeroCasterId;
    /** 零域结束的施术者个人回合。 */
    public int zeroUntilTurn;
    /** 最近建筑损坏前业主，产权变化后失效。 */
    public String damageOwnerId;
    /** 最近建筑损坏前等级。 */
    public int damageLevel;
    /** 四之弹修复窗口结束的业主个人回合。 */
    public int damageUntilTurn;
    /** 防止转手后再转回恢复旧损坏记录的产权观察值。 */
    public String observedOwnerId;
    /** 虚空之泪另一端，空表示无裂隙。 */
    public Integer portalTileId;
    /** 神威施术者，空值表示未封入异空间。 */
    public String kamuiCasterId;
    /** 神威剩余施术者个人回合。 */
    public int kamuiTurns;
    /** 神威施放时的全局行动编号，当回合不递减。 */
    public int kamuiAppliedTurn;
    /** 花瓣标记的触发者，空表示没有一次性租金修正。 */
    public String petalCasterId;
    /** 花瓣标记绑定业主，产权改变后失效。 */
    public String petalOwnerId;
    /** 标记在触发者该个人回合开始前有效。 */
    public int petalUntilTurn;
}
