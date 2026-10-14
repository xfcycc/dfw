package com.dfw.interfaces.dto;

import lombok.Getter;
import lombok.Setter;

/** 访客申请恢复房间角色的 JSON 输入。 */
@Getter
@Setter
public class SeatClaimInput {
    /** 访客会话原始令牌。 */
    public String token;
    /** 申请的保存角色。 */
    public String playerId;
    /** 可选申请人昵称。 */
    public String name;
}
