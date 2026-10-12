package com.dfw.interfaces;

import com.dfw.domain.GameError;
import com.dfw.interfaces.vo.CommandResult;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;

/** HTTP 错误响应保持原有 400 和稳定结果格式。 */
@RestControllerAdvice
public class ErrorAdvice {
    /** 输入请求异常，返回参数、业务或保存失败结果。 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommandResult> failure(Exception error) {
        if (error instanceof HttpMessageNotReadableException) error = new GameError("请求参数不正确", "INVALID_INPUT");
        return ResponseEntity.badRequest().body(Failures.result(error));
    }
}
