package com.dfw.domain;

/** 非地图随机源，由基础设施提供独立的服务端随机实现。 */
public interface RandomSource {
    /** 输入上界，返回 [0, bound) 的均匀随机整数。 */
    int nextInt(int bound);
    /** 返回独立随机标识。 */
    String id();
}
