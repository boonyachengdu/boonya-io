package com.boonya.lab.io.analytics.service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class EnergyAnalyticsService {

    public Map<String, Object> getOverview() {
        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("siteName", "Boonya 工业园区");
        overview.put("period", LocalDate.now().toString());
        overview.put("electricityKwh", 128640.8);
        overview.put("waterM3", 4862.5);
        overview.put("solarKwh", 32680.4);
        overview.put("storageDischargeKwh", 8450.2);
        overview.put("energyCostCny", 92680.6);
        overview.put("carbonTons", 72.38);
        overview.put("carbonReductionTons", 18.64);
        overview.put("activeAlarms", 7);
        overview.put("onlineDevices", 186);
        overview.put("totalDevices", 200);
        return overview;
    }

    public List<Map<String, Object>> getEnergyTrend(String period) {
        String[] labels = switch (period == null ? "day" : period.toLowerCase()) {
            case "month" -> new String[]{"01", "05", "10", "15", "20", "25", "30"};
            case "year" -> new String[]{"1月", "2月", "3月", "4月", "5月", "6月", "7月", "8月", "9月", "10月", "11月", "12月"};
            default -> new String[]{"00:00", "04:00", "08:00", "12:00", "16:00", "20:00", "24:00"};
        };

        double[] electricity = {5200, 4800, 6100, 7800, 8400, 7200, 6500, 6900, 7600, 8100, 7400, 7000};
        double[] water = {160, 150, 210, 280, 310, 260, 230, 245, 268, 292, 255, 240};
        double[] solar = {0, 0, 4200, 9300, 8600, 2600, 0, 0, 0, 0, 0, 0};

        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < labels.length; i++) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("time", labels[i]);
            item.put("electricityKwh", electricity[i]);
            item.put("waterM3", water[i]);
            item.put("solarKwh", solar[i]);
            item.put("carbonTons", round(electricity[i] * 0.0005703));
            result.add(item);
        }
        return result;
    }

    public List<Map<String, Object>> getAreaRanking() {
        return List.of(
                area("A1 生产车间", 38680.5, 1360.4, 22.06, "up"),
                area("A2 注塑车间", 29840.8, 980.6, 17.02, "stable"),
                area("B1 办公楼", 18620.3, 1240.2, 10.62, "down"),
                area("C1 仓储中心", 12680.9, 520.7, 7.23, "stable"),
                area("公共照明", 8240.1, 310.6, 4.7, "up")
        );
    }

    public List<Map<String, Object>> getDeviceStatus() {
        return List.of(
                device("meter-main-001", "园区总电表", "electric_meter", "online", 4280.6, "kW", "A1 配电房"),
                device("water-main-001", "园区总水表", "water_meter", "online", 86.4, "m3/h", "水泵房"),
                device("pv-inverter-001", "1号光伏逆变器", "solar_inverter", "online", 1560.2, "kW", "屋顶光伏一区"),
                device("ess-bms-001", "储能 BMS", "storage_bms", "warning", 72.5, "SOC", "储能站"),
                device("meter-a2-018", "A2 车间电表", "electric_meter", "offline", 0, "kW", "A2 注塑车间")
        );
    }

    public List<Map<String, Object>> getAlarms() {
        return List.of(
                alarm("high", "A2 车间电表夜间用电异常", "meter-a2-018", "持续 2 小时超过夜间基线 38%"),
                alarm("medium", "储能电池温度偏高", "ess-bms-001", "最高温度 43.8°C"),
                alarm("medium", "1号水表瞬时流量突增", "water-main-001", "15 分钟内流量环比增加 62%"),
                alarm("low", "3号光伏逆变器通信抖动", "pv-inverter-003", "最近 1 小时离线 2 次")
        );
    }

    private Map<String, Object> area(String name, double electricityKwh, double waterM3, double carbonTons, String trend) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("name", name);
        item.put("electricityKwh", electricityKwh);
        item.put("waterM3", waterM3);
        item.put("carbonTons", carbonTons);
        item.put("trend", trend);
        return item;
    }

    private Map<String, Object> device(String id, String name, String type, String status, double value, String unit, String location) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("deviceId", id);
        item.put("deviceName", name);
        item.put("deviceType", type);
        item.put("status", status);
        item.put("value", value);
        item.put("unit", unit);
        item.put("location", location);
        return item;
    }

    private Map<String, Object> alarm(String level, String title, String deviceId, String description) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("level", level);
        item.put("title", title);
        item.put("deviceId", deviceId);
        item.put("description", description);
        item.put("status", "open");
        item.put("time", LocalDate.now().toString());
        return item;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
