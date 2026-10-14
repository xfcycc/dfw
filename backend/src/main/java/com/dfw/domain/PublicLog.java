package com.dfw.domain;

import lombok.Getter;
import lombok.Setter;

/** 公开事件记录。 */
@Getter
@Setter
public class PublicLog {
    /** 公开记录类型，旧快照可缺省；chat 表示玩家对话。 */
    public String type;
    /** 文案中出现的当时玩家标记，旧记录缺省为空。 */
    public java.util.List<PlayerMention> mentions = new java.util.ArrayList<>();
    /** 记录标识。 */
    public String id;
    /** 不含私人信息的事件描述。 */
    public String text;
    /** 事件时间戳。 */
    public long at;
}
