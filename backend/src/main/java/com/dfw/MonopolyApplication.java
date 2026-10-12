package com.dfw;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** 大富翁单进程服务入口。 */
@EnableScheduling
@SpringBootApplication
public class MonopolyApplication {
    /** 输入启动参数，启动 HTTP、WebSocket 和持久化服务。 */
    public static void main(String[] args) { SpringApplication.run(MonopolyApplication.class, args); }
}
