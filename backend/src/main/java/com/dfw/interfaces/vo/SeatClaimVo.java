package com.dfw.interfaces.vo;

import lombok.Getter;
import lombok.Setter;

/** 仅房主可见的角色申请，不包含令牌摘要。 */
@Getter
@Setter
public class SeatClaimVo {
    /** 申请标识。 */
    public String id;
    /** 目标角色。 */
    public String playerId;
    /** 申请人自报昵称。 */
    public String name;
    /** 申请时间。 */
    public long at;
}
