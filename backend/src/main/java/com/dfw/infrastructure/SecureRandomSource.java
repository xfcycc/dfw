package com.dfw.infrastructure;

import com.dfw.domain.RandomSource;
import java.security.SecureRandom;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** 服务端骰子、抽卡与标识随机源。 */
@Component
public class SecureRandomSource implements RandomSource {
    private final SecureRandom random = new SecureRandom();
    /** 输入上界，返回均匀随机整数。 */
    public int nextInt(int bound) { return random.nextInt(bound); }
    /** 返回新的 UUID。 */
    public String id() { return UUID.randomUUID().toString(); }
}
