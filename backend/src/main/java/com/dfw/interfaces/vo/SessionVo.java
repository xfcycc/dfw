package com.dfw.interfaces.vo;

import lombok.Getter;
import lombok.Setter;

/** 进入房间的会话响应。 */
@Getter
@Setter
public class SessionVo {
    /** 永久房间标识。 */
    public String roomId;
    /** 玩家可见的固定存档码，纯观战会话为空。 */
    public String archiveCode;
    /** 恢复房间待分配访客；浏览器与纯观战身份分开保存。 */
    public boolean visitor;
    /** 房间号。 */
    public String code;
    /** 只返回浏览器的原始令牌。 */
    public String token;
    /** 玩家标识，观战为空。 */
    public String playerId;
}
