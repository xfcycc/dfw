package com.dfw.interfaces.dto;

import lombok.Getter;
import lombok.Setter;

/** 创建、加入和观战输入。 */
@Getter
@Setter
public class RoomInput {
    /** 加入或观战房间号。 */
    public String code;
    /** 可选昵称。 */
    public String name = "";
}
