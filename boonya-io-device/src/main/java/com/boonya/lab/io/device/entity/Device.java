package com.boonya.lab.io.device.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("device")
@Schema(description = "设备信息")
public class Device {

    @TableId(type = IdType.AUTO)
    @Schema(description = "设备ID", example = "1")
    private Long id;

    @Schema(description = "设备唯一标识", example = "sensor_001")
    private String deviceId;

    @Schema(description = "设备名称", example = "温度传感器1号")
    private String deviceName;

    @Schema(description = "设备类型", example = "temperature_sensor")
    private String deviceType;

    @Schema(description = "设备型号", example = "TMP-100")
    private String model;

    @Schema(description = "固件版本", example = "1.0.0")
    private String firmwareVersion;

    @Schema(description = "设备状态：online-在线, offline-离线, inactive-未激活, disabled-禁用")
    private String status;

    @Schema(description = "最后心跳时间")
    private LocalDateTime lastHeartbeat;

    @Schema(description = "所属分组ID")
    private Long groupId;

    // 修改内容：修改人：pengjunlin 时间：2026-08-04 19:00:00 -- start ----
    @Schema(description = "租户ID")
    private Long tenantId;
    // 修改内容：修改人：pengjunlin 时间：2026-08-04 19:00:00 -- end ----

    @Schema(description = "位置信息")
    private String location;

    @Schema(description = "描述信息")
    private String description;

    @Schema(description = "认证Token")
    private String authToken;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @TableLogic
    @Schema(description = "逻辑删除：0-未删除, 1-已删除")
    private Integer deleted;
}
