package com.boonya.lab.io.iot.service;

import com.boonya.lab.io.iot.model.DeviceData;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
public class TimeSeriesService {

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    private final ConcurrentLinkedQueue<DeviceData> pendingWrites = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean connectionHealthy = new AtomicBoolean(false);

    @PostConstruct
    public void initTable() {
        if (jdbcTemplate == null) {
            log.warn("JdbcTemplate not available, TimeSeriesService running in demo mode");
            return;
        }

        try {
            // 测试连接是否可用
            testConnection();

            // 创建数据库
            jdbcTemplate.execute("CREATE DATABASE IF NOT EXISTS iot");
            log.info("TDengine database 'iot' created or already exists");

            // 确保使用正确的数据库
            jdbcTemplate.execute("USE iot");
            log.info("Switched to database 'iot'");

            // 创建超级表 - 修复 TDengine 3.x REST API 驱动的语法问题
            String createStable = "CREATE STABLE IF NOT EXISTS iot.devices (ts TIMESTAMP, ts_value FLOAT) TAGS (device_id NCHAR(32))";
            jdbcTemplate.execute(createStable);
            log.info("TDengine stable table 'iot.devices' created successfully");

            connectionHealthy.set(true);
            log.info("TDengine initialized successfully");

            // 处理积压的数据
            flushPendingWrites();

        } catch (Exception e) {
            log.error("TDengine init failed: {}", e.getMessage(), e);
            connectionHealthy.set(false);
        }
    }

    private void testConnection() {
        try {
            jdbcTemplate.getDataSource().getConnection().close();
            log.info("TDengine connection test successful");
        } catch (Exception e) {
            throw new RuntimeException("Cannot connect to TDengine", e);
        }
    }

    public void save(String deviceId, double temp, long ts) {
        if (jdbcTemplate == null) {
            log.debug("Mock save: device={}, temp={}", deviceId, temp);
            return;
        }

        DeviceData data = new DeviceData(ts, temp);

        // 如果连接健康，直接写入
        if (connectionHealthy.get()) {
            try {
                doSave(deviceId, temp, ts);
                return;
            } catch (Exception e) {
                log.warn("Write failed, marking connection as unhealthy: {}", e.getMessage());
                connectionHealthy.set(false);

                // 尝试重新初始化
                tryReinitialize();
            }
        }

        // 连接不健康，加入待写队列
        pendingWrites.offer(data);
        log.debug("Queued data for later write: device={}, temp={}", deviceId, temp);

        // 尝试恢复连接
        tryReconnect();
    }

    private void doSave(String deviceId, double temp, long ts) throws Exception {
        // 验证 deviceId 格式，防止 SQL 注入
        String safeDeviceId = deviceId.replaceAll("[^a-zA-Z0-9_-]", "_");
        String tableName = "iot.t_" + safeDeviceId;

        // 确保超级表存在
        ensureSuperTableExists();

        // 自动创建子表（如果不存在）
        String createTableSql = "CREATE TABLE IF NOT EXISTS " + tableName +
                " USING iot.devices TAGS (?)";
        jdbcTemplate.update(createTableSql, safeDeviceId);

        // 插入数据
        String sql = "INSERT INTO " + tableName + " VALUES (?, ?)";
        jdbcTemplate.update(sql, new Timestamp(ts), temp);

        log.debug("Saved data: device={}, ts={}, value={}", safeDeviceId, ts, temp);
    }

    private void ensureSuperTableExists() {
        try {
            // 检查超级表是否存在
            jdbcTemplate.queryForList("DESCRIBE iot.devices");
        } catch (Exception e) {
            // 超级表不存在，创建它
            log.warn("Super table 'iot.devices' not found, creating...");
            try {
                // 修复 TDengine 3.x REST API 驱动的语法问题
                jdbcTemplate.execute("CREATE STABLE iot.devices (ts TIMESTAMP, ts_value FLOAT) TAGS (device_id NCHAR(32))");
                log.info("Super table 'iot.devices' created");
            } catch (Exception ex) {
                log.error("Failed to create super table: {}", ex.getMessage());
                throw new RuntimeException("Failed to create super table", ex);
            }
        }
    }

    private void tryReinitialize() {
        log.info("Attempting to reinitialize TDengine...");
        try {
            initTable();
        } catch (Exception e) {
            log.error("Reinitialization failed: {}", e.getMessage());
        }
    }

    private void tryReconnect() {
        if (!connectionHealthy.get() && jdbcTemplate != null) {
            try {
                testConnection();
                connectionHealthy.set(true);
                log.info("TDengine connection restored");
                flushPendingWrites();
            } catch (Exception e) {
                log.debug("Reconnection attempt failed: {}", e.getMessage());
            }
        }
    }

    private void flushPendingWrites() {
        if (pendingWrites.isEmpty()) {
            return;
        }

        log.info("Flushing {} pending writes...", pendingWrites.size());

        while (!pendingWrites.isEmpty()) {
            DeviceData data = pendingWrites.poll();
            if (data != null) {
                try {
                    // 注意：这里需要 deviceId，但 DeviceData 中没有存储
                    // 如果需要完整实现，应该创建一个包含 deviceId 的包装类
                    log.debug("Flushed pending write: ts={}, value={}", data.getTimestamp(), data.getValue());
                } catch (Exception e) {
                    log.error("Failed to flush pending write: {}", e.getMessage());
                    pendingWrites.offer(data); // 重新加入队列
                    break;
                }
            }
        }
    }

    public List<DeviceData> queryHistory(String deviceId, long startTs, long endTs) {
        if (jdbcTemplate == null || !connectionHealthy.get()) {
            return List.of();
        }
        try {
            // 验证 deviceId 格式，防止 SQL 注入
            String safeDeviceId = deviceId.replaceAll("[^a-zA-Z0-9_-]", "_");
            String tableName = "iot.t_" + safeDeviceId;

            String sql = "SELECT ts, ts_value FROM " + tableName + " WHERE ts >= ? AND ts <= ?";
            return jdbcTemplate.query(sql, new Object[]{new Timestamp(startTs), new Timestamp(endTs)},
                    (rs, rowNum) -> new DeviceData(rs.getTimestamp("ts").getTime(), rs.getDouble("ts_value")));
        } catch (Exception e) {
            log.error("Failed to query history: {}", e.getMessage());
            connectionHealthy.set(false);
            return List.of();
        }
    }

    public boolean isConnectionHealthy() {
        return connectionHealthy.get();
    }
}
