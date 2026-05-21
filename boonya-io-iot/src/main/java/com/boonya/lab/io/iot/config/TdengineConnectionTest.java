package com.boonya.lab.io.iot.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Slf4j
@Component
public class TdengineConnectionTest implements CommandLineRunner {

    @Autowired(required = false)
    private DataSource dataSource;

    @Override
    public void run(String... args) {
        if (dataSource == null) {
            log.warn("DataSource not configured");
            return;
        }

        try (Connection conn = dataSource.getConnection()) {
            log.info("✓ TDengine connection successful!");
            log.info("  URL: {}", conn.getMetaData().getURL());
            log.info("  Driver: {}", conn.getMetaData().getDriverName());
            log.info("  Version: {}", conn.getMetaData().getDatabaseProductVersion());
        } catch (SQLException e) {
            log.error("✗ TDengine connection failed: {}", e.getMessage(), e);
        }
    }
}
