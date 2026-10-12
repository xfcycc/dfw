package com.dfw.interfaces;

import com.dfw.domain.GameError;
import com.dfw.interfaces.vo.CommandResult;
import org.slf4j.*;

/** 不暴露数据库和内部状态的统一错误结果。 */
public final class Failures {
    private static final Logger LOG = LoggerFactory.getLogger(Failures.class);
    /** 输入异常，返回可展示业务错误或保存失败结果。 */
    public static CommandResult result(Exception error) {
        CommandResult result = new CommandResult(); result.ok = false;
        if (error instanceof GameError game) { result.code = game.code; result.error = game.getMessage(); }
        else { LOG.error("房间操作未提交", error); result.code = "SAVE_FAILED"; result.error = "保存或处理失败，请稍后重试；本次操作未提交"; }
        return result;
    }
}
