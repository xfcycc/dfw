package com.dfw.interfaces.vo;

import lombok.Getter;
import lombok.Setter;
import java.util.*;
/** 固定存档码对应的房间历史索引。 */
@Getter
@Setter
public class ArchiveVo {
    /** 内部房间标识。 */
    public String roomId;
    /** 规范化存档码。 */
    public String archiveCode;
    /** 原联机房间号。 */
    public String code;
    /** 恢复来源的存档码。 */
    public String sourceArchiveCode;
    /** 恢复来源保存点。 */
    public String sourceSnapshotId;
    /** 最新已提交进度。 */
    public ArchiveSnapshotVo latest;
    /** 按对局汇总的历史索引。 */
    public List<ArchiveGameVo> games = new ArrayList<>();
    /** 本页手动保存点。 */
    public List<ArchiveSnapshotVo> bookmarks = new ArrayList<>();
    /** 手动保存点下一页的版本游标。 */
    public Long nextCursor;
}
