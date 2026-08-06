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

        if (!testConnection()) {
            log.warn("TDengine not reachable, TimeSeriesService running in demo mode. " +
                    "Data will be queued and replayed once connection is restored.");
            connectionHealthy.set(false);
            return;
        }

        try {
            initDatabaseAndTables();
            flushPendingWrites();
        } catch (Exception e) {
            log.warn("TDengine init failed ({}), running in demo mode", e.getMessage());
            connectionHealthy.set(false);
        }
    }

    private void initDatabaseAndTables() {
        jdbcTemplate.execute("CREATE DATABASE IF NOT EXISTS iot");
        log.info("TDengine database 'iot' created or already exists");

        String createStable = "CREATE STABLE IF NOT EXISTS iot.devices (ts TIMESTAMP, ts_value FLOAT) TAGS (device_id NCHAR(32))";
        jdbcTemplate.execute(createStable);
        log.info("TDengine stable table 'iot.devices' created successfully");

        // 能碳专用超级表：支持电/水/光伏/储能多指标
        // metric_type: electricity / water / solar / storage
        // 注意：value 是 TDengine 保留关键字，列名使用 metric_value 避免冲突
        String createEnergyStable = "CREATE STABLE IF NOT EXISTS iot.energy_metrics " +
                "(ts TIMESTAMP, metric_value FLOAT) TAGS (device_id NCHAR(32), metric_type NCHAR(16))";
        jdbcTemplate.execute(createEnergyStable);
        log.info("TDengine stable table 'iot.energy_metrics' created successfully");

        connectionHealthy.set(true);
        log.info("TDengine initialized successfully");
    }

    private boolean testConnection() {
        try {
            jdbcTemplate.getDataSource().getConnection().close();
            log.info("TDengine connection test successful");
            return true;
        } catch (Exception e) {
            log.debug("TDengine connection test failed: {}", e.getMessage());
            return false;
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
        // 验证 deviceId 格式，防止 SQL 注入；TDengine 表名不允许连字符
        String safeDeviceId = deviceId.replaceAll("[^a-zA-Z0-9_]", "_");
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
        if (!connectionHealthy.get() && jdbcTemplate != null && testConnection()) {
            connectionHealthy.set(true);
            log.info("TDengine connection restored");
            try {
                initDatabaseAndTables();
                flushPendingWrites();
            } catch (Exception e) {
                log.warn("Reconnected but reinit failed: {}", e.getMessage());
                connectionHealthy.set(false);
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
            // 验证 deviceId 格式，防止 SQL 注入；TDengine 表名不允许连字符
            String safeDeviceId = deviceId.replaceAll("[^a-zA-Z0-9_]", "_");
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

    /**
     * 保存能碳指标数据到 iot.energy_metrics 超表。
     * value 语义：本次上报的增量值（电量 kWh / 水量 m³ / 光伏 kWh / 储能 kWh），
     * SUM(value) 即得周期内总量。
     *
     * @param deviceId   能碳设备 ID（如 meter-main-001）
     * @param metricType 指标类型（electricity / water / solar / storage）
     * @param value      本次增量值
     * @param ts         时间戳（毫秒）
     */
    public void saveEnergyMetric(String deviceId, String metricType, double value, long ts) {
        if (jdbcTemplate == null) {
            log.debug("Mock save energy: device={}, type={}, value={}", deviceId, metricType, value);
            return;
        }
        if (!connectionHealthy.get()) {
            tryReconnect();
            if (!connectionHealthy.get()) {
                log.warn("TDengine unavailable, skip energy metric: device={}, type={}", deviceId, metricType);
                return;
            }
        }
        try {
            doSaveEnergyMetric(deviceId, metricType, value, ts);
        } catch (Exception e) {
            log.warn("Energy write failed: {}", e.getMessage());
            connectionHealthy.set(false);
            tryReinitialize();
        }
    }

    private void doSaveEnergyMetric(String deviceId, String metricType, double value, long ts) throws Exception {
        // TDengine 表名只允许字母、数字、下划线，连字符会导致语法错误，全部替换为下划线
        String safeDeviceId = deviceId.replaceAll("[^a-zA-Z0-9_]", "_");
        String safeMetricType = metricType.replaceAll("[^a-zA-Z0-9_]", "_");
        // 子表命名：e_{deviceId}_{metricType}
        String tableName = "iot.e_" + safeDeviceId + "_" + safeMetricType;

        // 自动创建子表（USING energy_metrics TAGS(device_id, metric_type)）
        String createTableSql = "CREATE TABLE IF NOT EXISTS " + tableName +
                " USING iot.energy_metrics TAGS (?, ?)";
        jdbcTemplate.update(createTableSql, safeDeviceId, safeMetricType);

        // 插入数据（显式指定列名，避免超表列顺序不一致问题）
        String sql = "INSERT INTO " + tableName + " (ts, metric_value) VALUES (?, ?)";
        jdbcTemplate.update(sql, new Timestamp(ts), value);

        log.debug("Saved energy metric: device={}, type={}, ts={}, value={}", safeDeviceId, safeMetricType, ts, value);
    }

    public boolean isConnectionHealthy() {
        return connectionHealthy.get();
    }
}
