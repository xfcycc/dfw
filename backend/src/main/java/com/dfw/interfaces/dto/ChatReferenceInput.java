package com.dfw.interfaces.dto;

import lombok.Getter;
import lombok.Setter;

/** 聊天输入框选中的公开引用，不接受客户端指定玩家颜色。 */
@Getter
@Setter
public class ChatReferenceInput {
    /** 引用起始 UTF-16 偏移。 */
    public int start;
    /** 引用结束 UTF-16 偏移。 */
    public int end;
    /** 被艾特玩家身份，与地块编号二选一。 */
    public String playerId;
    /** 地块编号，与玩家身份二选一。 */
    public Integer tileId;
}
