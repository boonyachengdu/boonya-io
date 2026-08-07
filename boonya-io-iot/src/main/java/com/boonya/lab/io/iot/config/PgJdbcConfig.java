package com.boonya.lab.io.iot.config;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.lang.NonNull;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * PostgreSQL 数据源配置（用于告警持久化 device_alert / 规则持久化 alert_rule，连接 iot_device 库）。
 *
 * <p>iot 模块主数据源（spring.datasource）绑定的是 TDengine（由 TimeSeriesService 使用），
 * 这里单独建一个名为 "pgJdbcTemplate" 的 JdbcTemplate Bean，避免与主数据源冲突。</p>
 *
 * <p>仅当配置了 pg.datasource.url 时该配置才生效（{@link ConditionalOnProperty}）；
 * 未配置时 pgJdbcTemplate Bean 不会创建，AlertHandler / AlertRuleMapper 会自动降级为只推送不持久化。</p>
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "pg.datasource", name = "url")
public class PgJdbcConfig {

    @Value("${pg.datasource.url}")
    private String url;

    @Value("${pg.datasource.username:postgres}")
    private String username;

    @Value("${pg.datasource.password:boonya}")
    private String password;

    @Value("${pg.datasource.driver-class-name:org.postgresql.Driver}")
    private String driverClassName;

    @Value("${pg.datasource.hikari.maximum-pool-size:5}")
    private int maximumPoolSize;

    @Bean("pgDataSource")
    public DataSource pgDataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setPoolName("iot-pg-pool");
        ds.setJdbcUrl(url);
        ds.setUsername(username);
        ds.setPassword(password);
        ds.setDriverClassName(driverClassName);
        ds.setMaximumPoolSize(maximumPoolSize);
        // PG 不可用时不阻塞 iot 模块启动（与 TDengine 主数据源的降级策略保持一致）
        ds.setInitializationFailTimeout(-1);
        return ds;
    }

    @Bean("pgJdbcTemplate")
    public JdbcTemplate pgJdbcTemplate(@Qualifier("pgDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    /**
     * 启动时自动执行 schema-postgres.sql，幂等创建 device_alert / alert_rule 表。
     * 失败仅告警不影响启动。
     */
    @PostConstruct
    public void initSchema() {
        try (Connection conn = pgDataSource().getConnection()) {
            ScriptUtils.executeSqlScript(conn, new ClassPathResource("schema-postgres.sql"));
            log.info("PostgreSQL schema initialized from schema-postgres.sql (iot_device)");
        } catch (Exception e) {
            log.warn("PostgreSQL schema init skipped (iot_device): {}", e.getMessage());
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "PgJdbcConfig{url=" + url + "}";
    }
}
