package com.boonya.lab.io.device.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("device_alert")
@Schema(description = "设备告警")
public class Alert {

    @TableId(type = IdType.AUTO)
    @Schema(description = "告警ID")
    private Long id;

    @Schema(description = "设备唯一标识")
    private String deviceId;

    @Schema(description = "告警类型：TEMP_HIGH/TEMP_LOW/ENERGY_HIGH/CUSTOM")
    private String alertType;

    @Schema(description = "严重级别：INFO/WARNING/CRITICAL")
    private String severity;

    @Schema(description = "告警标题")
    private String title;

    @Schema(description = "告警消息")
    private String message;

    @Schema(description = "触发值")
    private Double metricValue;

    @Schema(description = "阈值")
    private Double threshold;

    @Schema(description = "状态：PENDING/ACKNOWLEDGED/RESOLVED/CLOSED")
    private String status;

    @Schema(description = "触发时间")
    private LocalDateTime triggerTime;

    @Schema(description = "确认时间")
    private LocalDateTime ackTime;

    @Schema(description = "解决时间")
    private LocalDateTime resolveTime;

    @Schema(description = "操作人")
    private String operator;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
