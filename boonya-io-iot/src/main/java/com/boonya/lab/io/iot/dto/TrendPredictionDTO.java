package com.boonya.lab.io.iot.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 温度趋势预测 DTO
 */
@Schema(description = "温度趋势预测")
public record TrendPredictionDTO(
        @Schema(description = "设备ID") String deviceId,
        @Schema(description = "预测分钟数") int predictMinutes,
        @Schema(description = "预测时间戳") long timestamp,
        @Schema(description = "状态：OK-成功, NO_DATA-数据不足") String status,
        @Schema(description = "提示消息（数据不足时）") String message,
        @Schema(description = "预测值") PredictedValue predicted,
        @Schema(description = "趋势：STABLE-稳定, RISING-上升, FALLING-下降") String trend,
        @Schema(description = "斜率") double slope,
        @Schema(description = "当前值") double currentValue,
        @Schema(description = "风险提示") List<String> risks
) {
    @Schema(description = "预测值")
    public record PredictedValue(
            @Schema(description = "预测值") double value,
            @Schema(description = "下界") double lowerBound,
            @Schema(description = "上界") double upperBound
    ) {}

    /**
     * 数据不足时的便捷构造
     */
    public static TrendPredictionDTO noData(String deviceId, int minutes, String message) {
        return new TrendPredictionDTO(
                deviceId, minutes, System.currentTimeMillis(),
                "NO_DATA", message,
                null, "STABLE", 0, 0, List.of()
        );
    }
}
