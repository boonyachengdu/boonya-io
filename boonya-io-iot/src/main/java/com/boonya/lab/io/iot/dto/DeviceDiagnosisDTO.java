package com.boonya.lab.io.iot.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 设备异常诊断报告 DTO
 */
@Schema(description = "设备异常诊断报告")
public record DeviceDiagnosisDTO(
        @Schema(description = "设备ID") String deviceId,
        @Schema(description = "诊断时间戳") long timestamp,
        @Schema(description = "诊断状态：NORMAL-正常, ABNORMAL-异常, NO_DATA-无数据") String status,
        @Schema(description = "提示消息（无数据时）") String message,
        @Schema(description = "数据点数量（无数据时为0）") int dataPoints,
        @Schema(description = "统计指标") Statistics statistics,
        @Schema(description = "异常列表") List<String> anomalies,
        @Schema(description = "异常数量") int anomalyCount,
        @Schema(description = "建议措施") List<String> suggestions
) {
    @Schema(description = "统计指标")
    public record Statistics(
            @Schema(description = "数据点数") int count,
            @Schema(description = "均值") double mean,
            @Schema(description = "标准差") double std,
            @Schema(description = "最大值") double max,
            @Schema(description = "最小值") double min,
            @Schema(description = "极差") double range
    ) {}

    /**
     * 无数据时的便捷构造
     */
    public static DeviceDiagnosisDTO noData(String deviceId, String message) {
        return new DeviceDiagnosisDTO(
                deviceId, System.currentTimeMillis(),
                "NO_DATA", message, 0,
                null, List.of(), 0, List.of()
        );
    }
}
