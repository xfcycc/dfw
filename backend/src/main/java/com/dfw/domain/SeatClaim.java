package com.dfw.domain;

import lombok.Getter;
import lombok.Setter;

/** 恢复房间的访客角色申请，仅保存令牌摘要。 */
@Getter
@Setter
public class SeatClaim {
    /** 申请标识。 */
    public String id;
    /** 访客会话摘要，不向客户端投影。 */
    public String tokenHash;
    /** 申请的保存角色。 */
    public String playerId;
    /** 申请人自报昵称，仅用于房主辨认。 */
    public String name;
    /** 申请时间。 */
    public long at;
}
