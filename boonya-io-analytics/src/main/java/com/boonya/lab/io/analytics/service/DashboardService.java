package com.boonya.lab.io.analytics.service;

import com.boonya.lab.io.analytics.dto.DeviceRealtimeData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 获取设备实时数据
     */
    public DeviceRealtimeData getDeviceRealtimeData(String deviceId) {
        String safeDeviceId = deviceId.replaceAll("[^a-zA-Z0-9_-]", "_");
        String tableName = "iot.t_" + safeDeviceId;

        try {
            // 查询最新数据
            String latestSql = "SELECT ts, ts_value FROM " + tableName + " ORDER BY ts DESC LIMIT 1";
            List<Map<String, Object>> latestRows = jdbcTemplate.queryForList(latestSql);

            if (latestRows.isEmpty()) {
                return DeviceRealtimeData.builder()
                        .deviceId(deviceId)
                        .build();
            }

            Map<String, Object> latest = latestRows.get(0);
            Double latestTemp = ((Number) latest.get("ts_value")).doubleValue();
            Long latestTimestamp = ((java.sql.Timestamp) latest.get("ts")).getTime();

            // 查询今日统计
            long todayStart = getTodayStartTimestamp();
            String statsSql = "SELECT AVG(ts_value) as avg_temp, MAX(ts_value) as max_temp, " +
                    "MIN(ts_value) as min_temp, COUNT(*) as count FROM " + tableName +
                    " WHERE ts >= ?";

            List<Map<String, Object>> statsRows = jdbcTemplate.queryForList(statsSql, new java.sql.Timestamp(todayStart));
            Map<String, Object> stats = statsRows.get(0);

            return DeviceRealtimeData.builder()
                    .deviceId(deviceId)
                    .latestTemp(latestTemp)
                    .latestTimestamp(latestTimestamp)
                    .todayAvgTemp(((Number) stats.get("avg_temp")).doubleValue())
                    .todayMaxTemp(((Number) stats.get("max_temp")).doubleValue())
                    .todayMinTemp(((Number) stats.get("min_temp")).doubleValue())
                    .dataPoints(((Number) stats.get("count")).intValue())
                    .build();

        } catch (Exception e) {
            log.error("Failed to get realtime data for device: {}", deviceId, e);
            return DeviceRealtimeData.builder().deviceId(deviceId).build();
        }
    }

    /**
     * 获取设备历史趋势数据
     */
    public List<Map<String, Object>> getDeviceTrend(String deviceId, String period) {
        String safeDeviceId = deviceId.replaceAll("[^a-zA-Z0-9_-]", "_");
        String tableName = "iot.t_" + safeDeviceId;

        long startTime = getPeriodStartTimestamp(period);
        String sql = "SELECT ts, ts_value FROM " + tableName + " WHERE ts >= ? ORDER BY ts ASC";

        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, new java.sql.Timestamp(startTime));

            List<Map<String, Object>> result = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                Map<String, Object> point = new HashMap<>();
                point.put("timestamp", ((java.sql.Timestamp) row.get("ts")).getTime());
                point.put("value", ((Number) row.get("ts_value")).doubleValue());
                result.add(point);
            }

            return result;
        } catch (Exception e) {
            log.error("Failed to get trend data for device: {}", deviceId, e);
            return new ArrayList<>();
        }
    }

    /**
     * 获取所有设备的概览统计
     */
    public Map<String, Object> getOverview() {
        Map<String, Object> overview = new HashMap<>();

        try {
            // TDengine 3.x REST 驱动不支持 COUNT(DISTINCT TBNAME)，改用子查询
            String deviceCountSql = "SELECT COUNT(*) FROM (SELECT DISTINCT TBNAME FROM iot.devices)";
            Integer deviceCount = jdbcTemplate.queryForObject(deviceCountSql, Integer.class);
            overview.put("totalDevices", deviceCount != null ? deviceCount : 0);

            // 查询今日数据点总数
            long todayStart = getTodayStartTimestamp();
            String dataPointsSql = "SELECT COUNT(*) FROM iot.devices WHERE ts >= ?";
            Integer dataPoints = jdbcTemplate.queryForObject(dataPointsSql, Integer.class, new java.sql.Timestamp(todayStart));
            overview.put("todayDataPoints", dataPoints != null ? dataPoints : 0);

            // 查询在线设备数（简化版，实际应从 Redis 获取）
            overview.put("onlineDevices", deviceCount != null ? deviceCount : 0);

        } catch (Exception e) {
            log.error("Failed to get overview statistics", e);
        }

        return overview;
    }

    private long getTodayStartTimestamp() {
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDateTime startOfDay = today.atStartOfDay();
        return java.sql.Timestamp.valueOf(startOfDay).getTime();
    }

    private long getPeriodStartTimestamp(String period) {
        long now = System.currentTimeMillis();
        return switch (period.toLowerCase()) {
            case "1h" -> now - 3600_000;
            case "6h" -> now - 6 * 3600_000;
            case "24h" -> now - 24 * 3600_000;
            case "7d" -> now - 7 * 24 * 3600_000;
            default -> now - 24 * 3600_000; // 默认 24 小时
        };
    }
}
