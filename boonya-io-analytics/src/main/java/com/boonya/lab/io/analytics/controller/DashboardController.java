package com.boonya.lab.io.analytics.controller;

import com.boonya.lab.io.analytics.dto.DeviceRealtimeData;
import com.boonya.lab.io.analytics.service.DashboardService;
import com.boonya.lab.io.common.response.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Tag(name = "数据分析", description = "设备数据看板、统计分析")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/device/{deviceId}/realtime")
    @Operation(summary = "获取设备实时数据", description = "获取指定设备的最新数据和今日统计")
    public Result<DeviceRealtimeData> getDeviceRealtime(@PathVariable String deviceId) {
        DeviceRealtimeData data = dashboardService.getDeviceRealtimeData(deviceId);
        return Result.success(data);
    }

    @GetMapping("/device/{deviceId}/trend")
    @Operation(summary = "获取设备趋势数据", description = "获取指定设备的历史趋势数据")
    public Result<List<Map<String, Object>>> getDeviceTrend(
            @PathVariable String deviceId,
            @RequestParam(defaultValue = "24h") String period) {
        List<Map<String, Object>> trend = dashboardService.getDeviceTrend(deviceId, period);
        return Result.success(trend);
    }

    @GetMapping("/overview")
    @Operation(summary = "获取系统概览", description = "获取所有设备的总体统计信息")
    public Result<Map<String, Object>> getOverview() {
        Map<String, Object> overview = dashboardService.getOverview();
        return Result.success(overview);
    }
}
