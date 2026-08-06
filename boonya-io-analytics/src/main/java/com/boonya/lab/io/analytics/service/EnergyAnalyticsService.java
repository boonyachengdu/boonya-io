package com.boonya.lab.io.analytics.service;

import com.boonya.lab.io.analytics.dto.AreaRankingDTO;
import com.boonya.lab.io.analytics.dto.EnergyAlarmDTO;
import com.boonya.lab.io.analytics.dto.EnergyDeviceStatusDTO;
import com.boonya.lab.io.analytics.dto.EnergyOverviewDTO;
import com.boonya.lab.io.analytics.dto.EnergyTrendItemDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 能碳分析服务 —— 基于 TDengine iot.energy_metrics 超表的真实聚合查询。
 * 数据来源：DeviceSimulator 模拟的能碳设备经 MQTT 上报，由 MqttSubscriber 写入。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnergyAnalyticsService {

    private final JdbcTemplate jdbcTemplate;

    // ===== 可调参数（提取到顶部便于调整）=====
    /** 电网碳排放因子（kgCO2/kWh），华东电网均值 */
    private static final double CARBON_FACTOR_KG_PER_KWH = 0.5703;
    /** 电价（元/kWh） */
    private static final double ELECTRICITY_PRICE = 0.72;
    /** 能碳告警阈值：电量设备单次增量(kWh) */
    private static final double ALARM_ELECTRICITY_THRESHOLD = 12.0;
    /** 能碳告警阈值：储能放电增量(kWh) */
    private static final double ALARM_STORAGE_THRESHOLD = 5.0;

    // ===== 能碳设备档案（与 DeviceSimulator 的 ENERGY_DEVICES 对齐）=====
    private static final List<DeviceProfile> DEVICE_PROFILES = List.of(
            new DeviceProfile("meter-main-001", "园区总电表", "electric_meter", "kW", "A1 配电房", "A1 生产车间", "electricity"),
            new DeviceProfile("meter-a2-018", "A2车间电表", "electric_meter", "kW", "A2 注塑车间", "A2 注塑车间", "electricity"),
            new DeviceProfile("water-main-001", "园区总水表", "water_meter", "m³/h", "水泵房", "公共照明", "water"),
            new DeviceProfile("pv-inverter-001", "1号光伏逆变器", "solar_inverter", "kW", "屋顶光伏一区", "B1 办公楼", "solar"),
            new DeviceProfile("ess-bms-001", "储能BMS", "storage_bms", "kW", "储能站", "C1 仓储中心", "storage")
    );

    /** 能碳总览：聚合今日各类指标 SUM(value) */
    public EnergyOverviewDTO getOverview() {
        long todayStart = getTodayStartTimestamp();
        double electricity = sumMetric("electricity", todayStart);
        double water = sumMetric("water", todayStart);
        double solar = sumMetric("solar", todayStart);
        double storage = sumMetric("storage", todayStart);

        double carbonTons = round(electricity * CARBON_FACTOR_KG_PER_KWH / 1000);
        double carbonReductionTons = round(solar * CARBON_FACTOR_KG_PER_KWH / 1000);
        double cost = round(electricity * ELECTRICITY_PRICE);

        int totalDevices = DEVICE_PROFILES.size();
        int onlineDevices = countOnlineDevices();
        int activeAlarms = getAlarms().size();

        return new EnergyOverviewDTO(
                "Boonya 工业园区",
                LocalDate.now().toString(),
                round(electricity), round(water), round(solar), round(storage),
                cost, carbonTons, carbonReductionTons,
                activeAlarms, onlineDevices, totalDevices
        );
    }

    /** 能源趋势：按时段 INTERVAL 聚合电/水/光伏 */
    public List<EnergyTrendItemDTO> getEnergyTrend(String period) {
        String p = period == null ? "day" : period.toLowerCase();
        long start = getPeriodStartTimestamp(p);
        String interval = switch (p) {
            case "month", "year" -> "1d";
            default -> "1h";
        };

        // ts -> [electricity, water, solar]，按时间窗口对齐
        TreeMap<Long, double[]> buckets = new TreeMap<>();
        fillTrend(buckets, "electricity", start, interval, 0);
        fillTrend(buckets, "water", start, interval, 1);
        fillTrend(buckets, "solar", start, interval, 2);

        List<EnergyTrendItemDTO> result = new ArrayList<>();
        buckets.forEach((ts, vals) -> result.add(new EnergyTrendItemDTO(
                formatTrendTime(ts, interval),
                round(vals[0]), round(vals[1]), round(vals[2]),
                round(vals[0] * CARBON_FACTOR_KG_PER_KWH / 1000)
        )));
        return result;
    }

    /** 区域能耗排行：按设备档案区域聚合今日电量/水量/碳排 */
    public List<AreaRankingDTO> getAreaRanking() {
        long todayStart = getTodayStartTimestamp();
        List<AreaRankingDTO> result = new ArrayList<>();
        for (DeviceProfile dp : DEVICE_PROFILES) {
            double elec = "electricity".equals(dp.metricType) ? sumDeviceMetric(dp.deviceId, "electricity", todayStart) : 0;
            double water = "water".equals(dp.metricType) ? sumDeviceMetric(dp.deviceId, "water", todayStart) : 0;
            double carbon = round(elec * CARBON_FACTOR_KG_PER_KWH / 1000);
            result.add(new AreaRankingDTO(dp.area, round(elec), round(water), carbon, "stable"));
        }
        return result;
    }

    /** 能碳设备状态：查每台设备最新上报值 + 合并档案 */
    public List<EnergyDeviceStatusDTO> getDeviceStatus() {
        List<EnergyDeviceStatusDTO> result = new ArrayList<>();
        for (DeviceProfile dp : DEVICE_PROFILES) {
            double latest = queryLatestValue(dp.deviceId);
            String status = latest > 0 ? "online" : "offline";
            result.add(new EnergyDeviceStatusDTO(
                    dp.deviceId, dp.name, dp.type, status, round(latest), dp.unit, dp.location
            ));
        }
        return result;
    }

    /** 能耗告警：基于最新值的阈值检测 */
    public List<EnergyAlarmDTO> getAlarms() {
        List<EnergyAlarmDTO> alarms = new ArrayList<>();
        int hour = LocalTime.now().getHour();
        String today = LocalDate.now().toString();

        for (DeviceProfile dp : DEVICE_PROFILES) {
            double latest = queryLatestValue(dp.deviceId);
            if ("electric_meter".equals(dp.type) && latest > ALARM_ELECTRICITY_THRESHOLD) {
                alarms.add(new EnergyAlarmDTO("medium", dp.name + "瞬时用电偏高",
                        dp.deviceId, "当前增量 " + round(latest) + " kWh", "open", today));
            }
            if ("solar_inverter".equals(dp.type) && (hour < 6 || hour >= 18) && latest > 0) {
                alarms.add(new EnergyAlarmDTO("low", dp.name + "夜间发电异常",
                        dp.deviceId, "夜间光伏上报 " + round(latest) + " kWh", "open", today));
            }
            if ("storage_bms".equals(dp.type) && latest > ALARM_STORAGE_THRESHOLD) {
                alarms.add(new EnergyAlarmDTO("medium", dp.name + "放电偏高",
                        dp.deviceId, "当前增量 " + round(latest) + " kWh", "open", today));
            }
        }
        return alarms;
    }

    // ===== 内部查询方法（均容错，TDengine 不可用时返回 0/空）=====

    /** 聚合某 metric_type 的 SUM(value) */
    private double sumMetric(String metricType, long startTs) {
        try {
            String sql = "SELECT SUM(metric_value) FROM iot.energy_metrics WHERE metric_type = ? AND ts >= ?";
            Double r = jdbcTemplate.queryForObject(sql, Double.class, metricType, new Timestamp(startTs));
            return r != null ? r : 0;
        } catch (Exception e) {
            log.debug("sumMetric failed [{}]: {}", metricType, e.getMessage());
            return 0;
        }
    }

    /** 聚合指定设备某 metric 的 SUM(value) */
    private double sumDeviceMetric(String deviceId, String metricType, long startTs) {
        try {
            String sql = "SELECT SUM(metric_value) FROM iot.energy_metrics WHERE device_id = ? AND metric_type = ? AND ts >= ?";
            Double r = jdbcTemplate.queryForObject(sql, Double.class, deviceId, metricType, new Timestamp(startTs));
            return r != null ? r : 0;
        } catch (Exception e) {
            log.debug("sumDeviceMetric failed [{},{}]: {}", deviceId, metricType, e.getMessage());
            return 0;
        }
    }

    /** 查询设备最新上报值（LAST） */
    private double queryLatestValue(String deviceId) {
        try {
            String sql = "SELECT LAST(metric_value) as v FROM iot.energy_metrics WHERE device_id = ?";
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, deviceId);
            if (rows.isEmpty()) return 0;
            Object v = rows.get(0).get("v");
            return v == null ? 0 : ((Number) v).doubleValue();
        } catch (Exception e) {
            log.debug("queryLatestValue failed [{}]: {}", deviceId, e.getMessage());
            return 0;
        }
    }

    /** 统计在线设备数（有历史数据即视为在线） */
    private int countOnlineDevices() {
        int count = 0;
        for (DeviceProfile dp : DEVICE_PROFILES) {
            if (queryLatestValue(dp.deviceId) > 0) {
                count++;
            }
        }
        return count;
    }

    /** 填充趋势 buckets：按 INTERVAL 聚合某 metric */
    private void fillTrend(TreeMap<Long, double[]> buckets, String metricType, long start, String interval, int idx) {
        try {
            // INTERVAL 不支持占位符，interval 来自固定 switch 白名单，安全拼接
            String sql = "SELECT _wstart as w, SUM(metric_value) as v FROM iot.energy_metrics " +
                    "WHERE metric_type = ? AND ts >= ? INTERVAL(" + interval + ")";
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, metricType, new Timestamp(start));
            for (Map<String, Object> row : rows) {
                long ts = ((Timestamp) row.get("w")).getTime();
                double v = ((Number) row.get("v")).doubleValue();
                buckets.computeIfAbsent(ts, k -> new double[3])[idx] = v;
            }
        } catch (Exception e) {
            log.debug("fillTrend failed [{}]: {}", metricType, e.getMessage());
        }
    }

    private long getTodayStartTimestamp() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        return Timestamp.valueOf(startOfDay).getTime();
    }

    private long getPeriodStartTimestamp(String period) {
        long now = System.currentTimeMillis();
        return switch (period) {
            case "month" -> now - 30L * 24 * 3600_000;
            case "year" -> now - 365L * 24 * 3600_000;
            default -> now - 24 * 3600_000; // day
        };
    }

    private String formatTrendTime(long ts, String interval) {
        java.util.Date d = new java.util.Date(ts);
        if ("1d".equals(interval)) {
            return LocalDate.of(1900 + d.getYear(), d.getMonth() + 1, d.getDate()).toString();
        }
        // 1h: 显示 HH:00
        return String.format("%02d:00", d.getHours());
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    /** 能碳设备档案（内部） */
    private record DeviceProfile(String deviceId, String name, String type,
                                 String unit, String location, String area, String metricType) {}
}
