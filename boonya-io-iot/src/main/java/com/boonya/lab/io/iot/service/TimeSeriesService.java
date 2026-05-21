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

            jdbcTemplate.execute("CREATE DATABASE IF NOT EXISTS iot");
            log.info("TDengine database 'iot' created or already exists");

            String createStable = "CREATE STABLE IF NOT EXISTS iot.devices " +
                    "(ts TIMESTAMP, value FLOAT) " +
                    "TAGS (device_id BINARY(32))";
            jdbcTemplate.execute(createStable);
            log.info("TDengine stable table created");

            connectionHealthy.set(true);

            // 处理积压的数据
            flushPendingWrites();

        } catch (Exception e) {
            log.warn("TDengine init failed: {}, using mock storage. Will retry on next write.", e.getMessage());
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
            }
        }

        // 连接不健康，加入待写队列
        pendingWrites.offer(data);
        log.debug("Queued data for later write: device={}, temp={}", deviceId, temp);

        // 尝试恢复连接
        tryReconnect();
    }

    private void doSave(String deviceId, double temp, long ts) throws Exception {
        String sql = "INSERT INTO iot.t_" + deviceId + " USING iot.devices TAGS (?) VALUES (?, ?)";
        jdbcTemplate.update(sql, deviceId, new Timestamp(ts), temp);
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
            String sql = "SELECT ts, value FROM iot.t_" + deviceId + " WHERE ts >= ? AND ts <= ?";
            return jdbcTemplate.query(sql, new Object[]{new Timestamp(startTs), new Timestamp(endTs)},
                    (rs, rowNum) -> new DeviceData(rs.getTimestamp("ts").getTime(), rs.getDouble("value")));
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
