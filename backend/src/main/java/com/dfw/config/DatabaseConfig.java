package com.dfw.config;

import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;

/** 房间存储和 Flowable 使用同一个数据源及事务管理器。 */
@Configuration
public class DatabaseConfig {
    /** 输入 MySQL 数据源，返回流程和房间存储共用的事务管理器。 */
    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
    /** 输入共享事务管理器，返回包含流程、快照与回执的短事务模板。 */
    @Bean
    public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }
}
