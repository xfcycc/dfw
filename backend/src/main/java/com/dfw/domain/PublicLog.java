package com.dfw.domain;

import lombok.Getter;
import lombok.Setter;

/** 公开事件记录。 */
@Getter
@Setter
public class PublicLog {
    /** 记录标识。 */
    public String id;
    /** 不含私人信息的事件描述。 */
    public String text;
    /** 事件时间戳。 */
    public long at;
}
