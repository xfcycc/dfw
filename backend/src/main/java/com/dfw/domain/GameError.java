package com.dfw.domain;

/** 可向客户端展示的业务错误。 */
public class GameError extends RuntimeException {
    public final String code;
    /** 输入可展示文案，创建业务规则错误。 */
    public GameError(String message) { this(message, "INVALID_ACTION"); }
    /** 输入文案和稳定错误码，创建业务错误。 */
    public GameError(String message, String code) { super(message); this.code = code; }
    /** 输入条件和文案，不满足时拒绝候选操作。 */
    public static void require(boolean condition, String message) { if (!condition) throw new GameError(message); }
}
