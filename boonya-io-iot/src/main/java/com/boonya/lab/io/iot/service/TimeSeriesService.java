package com.boonya.lab.io.iot.service;

import com.boonya.lab.io.iot.model.DeviceData;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;

@Slf4j
@Service
public class TimeSeriesService {

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void initTable() {
        if (jdbcTemplate == null) {
            log.warn("JdbcTemplate not available, TimeSeriesService running in demo mode");
            return;
        }
        try {
            // 创建超级表
            String createStable = "CREATE STABLE IF NOT EXISTS devices " +
                    "(ts TIMESTAMP, value FLOAT) " +
                    "TAGS (device_id BINARY(32))";
            jdbcTemplate.execute(createStable);
            log.info("TDengine stable table created");
        } catch (Exception e) {
            log.warn("TDengine init failed: {}, using mock storage", e.getMessage());
        }
    }

    public void save(String deviceId, double temp, long ts) {
        if (jdbcTemplate == null) {
            log.debug("Mock save: device={}, temp={}", deviceId, temp);
            return;
        }
        try {
            String sql = "INSERT INTO t_" + deviceId + " USING devices TAGS (?) VALUES (?, ?)";
            jdbcTemplate.update(sql, deviceId, new Timestamp(ts), temp);
        } catch (Exception e) {
            log.error("Failed to save temperature: {}", e.getMessage());
        }
    }

    public List<DeviceData> queryHistory(String deviceId, long startTs, long endTs) {
        if (jdbcTemplate == null) {
            return List.of(); // 返回空列表
        }
        try {
            String sql = "SELECT ts, value FROM t_" + deviceId + " WHERE ts >= ? AND ts <= ?";
            return jdbcTemplate.query(sql, new Object[]{new Timestamp(startTs), new Timestamp(endTs)},
                    (rs, rowNum) -> new DeviceData(rs.getTimestamp("ts").getTime(), rs.getDouble("value")));
        } catch (Exception e) {
            log.error("Failed to query history: {}", e.getMessage());
            return List.of();
        }
    }
}