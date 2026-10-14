package com.dfw.interfaces.vo;

import lombok.Getter;
import lombok.Setter;

/** 公开保存点及其恢复兼容性。 */
@Getter
@Setter
public class ArchiveSnapshotVo {
    /** 保存点标识。 */
    public String snapshotId;
    /** 所属对局，大厅为空。 */
    public String gameId;
    /** 提交版本。 */
    public long version;
    /** 行动编号。 */
    public int turnNumber;
    /** 保存时间。 */
    public long at;
    /** 保存时的房间阶段。 */
    public String status;
    /** 手动备注。 */
    public String label;
    /** 是否手动标记。 */
    public boolean manual;
    /** 是否与当前格式及规则兼容。 */
    public boolean restorable;
    /** 不可恢复的原因。 */
    public String restoreError;
    /** 仅预览接口及最新进度返回的公开历史局面。 */
    public PublicRoomVo state;
}
