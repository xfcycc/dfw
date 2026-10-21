package com.dfw.domain;

/** 非地图随机源，由基础设施提供独立的服务端随机实现。 */
public interface RandomSource {
    /** 输入上界，返回 [0, bound) 的均匀随机整数。 */
    int nextInt(int bound);
    /** 输入正长整型上界，拒绝采样返回均匀整数，兼容旧随机源实现。 */
    default long nextLong(long bound) {
        if(bound<=0) throw new IllegalArgumentException("随机上界须为正数");
        if(bound<=Integer.MAX_VALUE) return nextInt((int)bound);
        long limit=Long.MAX_VALUE-Long.MAX_VALUE%bound, sample;
        do { sample=((long)nextInt(1<<21)<<42)|((long)nextInt(1<<21)<<21)|nextInt(1<<21); } while(sample>=limit);
        return sample%bound;
    }
    /** 返回独立随机标识。 */
    String id();
}
