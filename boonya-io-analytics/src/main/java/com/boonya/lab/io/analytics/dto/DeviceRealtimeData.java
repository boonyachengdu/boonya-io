package com.boonya.lab.io.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "设备实时数据")
public class DeviceRealtimeData {

    @Schema(description = "设备ID")
    private String deviceId;

    @Schema(description = "最新温度值")
    private Double latestTemp;

    @Schema(description = "最新时间戳")
    private Long latestTimestamp;

    @Schema(description = "今日平均温度")
    private Double todayAvgTemp;

    @Schema(description = "今日最高温度")
    private Double todayMaxTemp;

    @Schema(description = "今日最低温度")
    private Double todayMinTemp;

    @Schema(description = "数据点数量")
    private Integer dataPoints;
}
