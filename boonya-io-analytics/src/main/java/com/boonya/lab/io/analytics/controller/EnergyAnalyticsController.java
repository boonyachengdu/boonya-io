package com.boonya.lab.io.analytics.controller;

import com.boonya.lab.io.analytics.service.EnergyAnalyticsService;
import com.boonya.lab.io.common.response.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics/energy")
@RequiredArgsConstructor
@Tag(name = "能碳分析", description = "能源、碳排、光伏、储能和能耗告警分析")
public class EnergyAnalyticsController {

    private final EnergyAnalyticsService energyAnalyticsService;

    @GetMapping("/overview")
    @Operation(summary = "获取能碳总览")
    public Result<Map<String, Object>> getOverview() {
        return Result.success(energyAnalyticsService.getOverview());
    }

    @GetMapping("/trend")
    @Operation(summary = "获取能源趋势")
    public Result<List<Map<String, Object>>> getTrend(@RequestParam(defaultValue = "day") String period) {
        return Result.success(energyAnalyticsService.getEnergyTrend(period));
    }

    @GetMapping("/areas/ranking")
    @Operation(summary = "获取区域能耗排行")
    public Result<List<Map<String, Object>>> getAreaRanking() {
        return Result.success(energyAnalyticsService.getAreaRanking());
    }

    @GetMapping("/devices/status")
    @Operation(summary = "获取能源设备状态")
    public Result<List<Map<String, Object>>> getDeviceStatus() {
        return Result.success(energyAnalyticsService.getDeviceStatus());
    }

    @GetMapping("/alarms")
    @Operation(summary = "获取能耗告警")
    public Result<List<Map<String, Object>>> getAlarms() {
        return Result.success(energyAnalyticsService.getAlarms());
    }
}
