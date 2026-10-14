package com.dfw.interfaces.vo;

import lombok.Getter;
import lombok.Setter;

/** 历史事实的公开白名单，不返回内部参数与私人结果。 */
@Getter
@Setter
public class HistoryEntryVo {
    /** 全局递增分页序号。 */
    public long sequence;
    /** 关联的提交完成保存点。 */
    public String snapshotId;
    /** 事实所属对局。 */
    public String gameId;
    /** 提交完成版本。 */
    public long version;
    /** 事实发生时的行动编号，旧日志未保存时为空。 */
    public Integer turnNumber;
    /** 发生阶段。 */
    public String phase;
    /** 作用角色。 */
    public String actorId;
    /** 当时角色昵称。 */
    public String actorName;
    /** 事实类型。 */
    public String type;
    /** 公开结果说明。 */
    public String text;
    /** 事实发生时间。 */
    public long at;
}
