package com.dfw.interfaces.dto;

import lombok.Getter;
import lombok.Setter;

/** 房间实时公开记录的会话及分页输入，不使用共享恢复凭证。 */
@Getter
@Setter
public class RoomLogInput {
    /** 当前玩家或观战会话令牌。 */
    public String token;
    /** 更早记录的序号游标，省略读取最新一页。 */
    public Long cursor;
}
