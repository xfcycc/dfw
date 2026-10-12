package com.dfw.interfaces.vo;

import lombok.Getter;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Setter;

/** 命令或请求结果。 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
public class CommandResult {
    /** 是否提交成功。 */
    public boolean ok;
    /** 成功时的版本。 */
    public Long version;
    /** 可展示错误。 */
    public String error;
    /** 稳定错误码。 */
    public String code;
}
