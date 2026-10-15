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
}
