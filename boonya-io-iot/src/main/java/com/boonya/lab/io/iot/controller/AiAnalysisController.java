package com.boonya.lab.io.iot.controller;

import com.boonya.lab.io.common.response.Result;
import com.boonya.lab.io.iot.service.AiAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// 修改内容：修改人：pengjunlin 时间：2026-08-04 18:30:00 -- start ----
/**
 * AI 智能分析接口
 * 提供基于规则与统计的设备异常诊断和趋势预测能力
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "AI智能分析", description = "设备数据AI分析与异常诊断")
public class AiAnalysisController {

    private final AiAnalysisService aiAnalysisService;

    @GetMapping("/device/{deviceId}/diagnosis")
    @Operation(summary = "设备异常诊断", description = "基于设备历史数据进行AI异常诊断")
    public Result<Map<String, Object>> diagnoseDevice(@PathVariable String deviceId) {
        return Result.success(aiAnalysisService.diagnoseDevice(deviceId));
    }

    @PostMapping("/device/{deviceId}/predict")
    @Operation(summary = "温度趋势预测", description = "预测设备未来温度趋势")
    public Result<Map<String, Object>> predictTrend(
            @PathVariable String deviceId,
            @RequestParam(defaultValue = "60") int minutes) {
        return Result.success(aiAnalysisService.predictTrend(deviceId, minutes));
    }
}
// 修改内容：修改人：pengjunlin 时间：2026-08-04 18:30:00 -- end ----
