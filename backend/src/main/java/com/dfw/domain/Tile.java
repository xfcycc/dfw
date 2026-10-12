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
    /** 地标名称。 */
    public String name = "";
    /** 街区编号。 */
    public int district;
    /** 地价。 */
    public long price;
    /** 所有者，空为无主。 */
    public String ownerId;
    /** 建筑等级。 */
    public int level;
    /** 是否抵押。 */
    public boolean mortgaged;
    /** 是否有路障。 */
    public boolean barrier;
}
