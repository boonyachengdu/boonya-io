package com.boonya.lab.io.device.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("device_log")
@Schema(description = "设备日志")
public class DeviceLog {

    @TableId(type = IdType.AUTO)
    @Schema(description = "日志ID")
    private Long id;

    @Schema(description = "设备ID")
    private String deviceId;

    @Schema(description = "日志类型：info, warning, error")
    private String logType;

    @Schema(description = "日志内容")
    private String message;

    @Schema(description = "日志详情（JSON）")
    private String detail;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
