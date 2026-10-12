package com.dfw.interfaces.vo;

import lombok.Getter;
import lombok.Setter;

/** 进入房间的会话响应。 */
@Getter
@Setter
public class SessionVo {
    /** 房间号。 */
    public String code;
    /** 只返回浏览器的原始令牌。 */
    public String token;
    /** 玩家标识，观战为空。 */
    public String playerId;
}
