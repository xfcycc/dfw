package com.dfw.infrastructure;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

/** 只持久化 SHA-256 摘要的随机身份令牌。 */
@Component
public class SessionTokens {
    private final SecureRandom random = new SecureRandom();
    /** 返回新的 32 字节随机十六进制令牌。 */
    public String issue() { byte[] bytes = new byte[32]; random.nextBytes(bytes); return HexFormat.of().formatHex(bytes); }
    /** 输入共享存档码、随机恢复请求 ID 和新房间 ID，派生可重复返回的会话；数据库仍只存摘要。 */
    public String restored(String archiveCode, String requestId, String roomId) { return hash("dfw:restore:" + archiveCode + ":" + requestId + ":" + roomId); }
    /** 输入原始令牌，返回与旧 Node 后端一致的摘要。 */
    public String hash(String token) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException error) { throw new IllegalStateException(error); }
    }
}
