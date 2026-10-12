package com.dfw.interfaces.dto;

import lombok.Getter;
import lombok.Setter;

/** 带版本和去重标识的命令。 */
@Getter
@Setter
public class GameCommand {
    /** 幂等命令标识。 */
    public String id;
    /** 客户端预期版本。 */
    public Long expectedVersion;
    /** 业务操作。 */
    public GameAction action;
}
