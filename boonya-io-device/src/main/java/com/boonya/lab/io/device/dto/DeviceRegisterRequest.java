package com.boonya.lab.io.device.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "设备注册请求")
public class DeviceRegisterRequest {

    @NotBlank(message = "设备ID不能为空")
    @Schema(description = "设备唯一标识", example = "sensor_001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String deviceId;

    @NotBlank(message = "设备名称不能为空")
    @Schema(description = "设备名称", example = "温度传感器1号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String deviceName;

    @Schema(description = "设备类型", example = "temperature_sensor")
    private String deviceType;

    @Schema(description = "设备型号", example = "TMP-100")
    private String model;

    @Schema(description = "所属分组ID")
    private Long groupId;

    @Schema(description = "位置信息", example = "车间A-区域1")
    private String location;

    @Schema(description = "描述信息")
    private String description;
}
