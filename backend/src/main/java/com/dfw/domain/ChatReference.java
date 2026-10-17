package com.dfw.domain;

import lombok.Getter;
import lombok.Setter;

/** 聊天正文中的显式引用；位置按 UTF-16 字符偏移保存，目标身份不随改名变化。 */
@Getter
@Setter
public class ChatReference {
    /** 引用起始位置，包含 @。 */
    public int start;
    /** 引用结束位置，不包含末尾分隔空格。 */
    public int end;
    /** 被艾特玩家的固定身份，与地块编号二选一。 */
    public String playerId;
    /** 被引用的地块编号，与玩家身份二选一。 */
    public Integer tileId;
    /** 服务端冻结的玩家颜色，地块引用为空。 */
    public String color;
}
