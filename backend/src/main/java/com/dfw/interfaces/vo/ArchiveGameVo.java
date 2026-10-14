package com.dfw.interfaces.vo;

import lombok.Getter;
import lombok.Setter;

/** 同一房间内按对局汇总的历史索引。 */
@Getter
@Setter
public class ArchiveGameVo {
    /** 对局标识，大厅为空。 */
    public String gameId;
    /** 首次记录时间。 */
    public long firstAt;
    /** 最后记录时间。 */
    public long lastAt;
    /** 已记录的最大行动编号。 */
    public int turnNumber;
    /** 保存点数量。 */
    public long snapshots;
}
