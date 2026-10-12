package com.dfw.config;

import com.dfw.infrastructure.flowable.GameWorkflowDelegates;
import com.dfw.infrastructure.flowable.GameWorkflowScope;
import javax.sql.DataSource;
import java.sql.SQLException;
import org.flowable.engine.ProcessEngine;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/** 仅接入嵌入式 BPMN 引擎，与游戏存储共用 Spring JDBC 事务。 */
@Configuration
public class FlowableConfig {
    /** 输入共享数据源、事务管理器和命令作用域，创建引擎并部署固定通用流程。 */
    @Bean(destroyMethod = "close")
    public ProcessEngine gameProcessEngine(DataSource dataSource, PlatformTransactionManager transactionManager,
            GameWorkflowScope scope, @Value("${game.flowable.schema-update:true}") String schemaUpdate) throws SQLException {
        SpringProcessEngineConfiguration config = new SpringProcessEngineConfiguration();
        config.setDataSource(dataSource); config.setTransactionManager(transactionManager);
        // 元数据只检查本数据源的库，避免同一 MySQL 上其他库的 ACT_* 表造成误判。
        try (var connection = dataSource.getConnection()) { config.setDatabaseCatalog(connection.getCatalog()); }
        config.setDatabaseSchemaUpdate(schemaUpdate); config.setAsyncExecutorActivate(false);
        config.setDisableIdmEngine(true); config.setDisableEventRegistry(true);
        config.setHistory("none"); config.setBeans(GameWorkflowDelegates.beans(scope));
        ProcessEngine engine = config.buildProcessEngine();
        try {
            engine.getRepositoryService().createDeployment().name("generic-game-runtime-v1")
                    .enableDuplicateFiltering().addClasspathResource("processes/generic-game-runtime.bpmn20.xml").deploy();
            return engine;
        } catch (RuntimeException error) { engine.close(); throw error; }
    }
}
