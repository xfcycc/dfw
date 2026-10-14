package com.dfw.interfaces.dto;

import lombok.Getter;
import lombok.Setter;

/** 档案查询、标记和恢复的 JSON 输入，按接口校验所用字段。 */
@Getter
@Setter
public class ArchiveInput {
    /** 固定共享存档码。 */
    public String archiveCode;
    /** 可选保存点，恢复时缺省使用最新进度。 */
    public String snapshotId;
    /** 可选对局筛选。 */
    public String gameId;
    /** 可选历史类型。 */
    public String type;
    /** 分页游标。 */
    public Long cursor;
    /** 可选行动编号。 */
    public Integer turnNumber;
    /** 仅标记保存点时验证当前玩家会话。 */
    public String token;
    /** 标记时使用的最新状态版本。 */
    public Long expectedVersion;
    /** 可选存档备注。 */
    public String label;
    /** 恢复请求去重标识。 */
    public String requestId;
    /** 恢复者选用的保存角色，缺省为原房主。 */
    public String playerId;
}
