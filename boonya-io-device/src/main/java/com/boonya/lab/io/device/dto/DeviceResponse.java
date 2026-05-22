package com.boonya.lab.io.device.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "设备响应")
public class DeviceResponse {

    @Schema(description = "设备ID")
    private Long id;

    @Schema(description = "设备唯一标识")
    private String deviceId;

    @Schema(description = "设备名称")
    private String deviceName;

    @Schema(description = "设备类型")
    private String deviceType;

    @Schema(description = "设备型号")
    private String model;

    @Schema(description = "固件版本")
    private String firmwareVersion;

    @Schema(description = "设备状态")
    private String status;

    @Schema(description = "最后心跳时间")
    private String lastHeartbeat;

    @Schema(description = "所属分组ID")
    private Long groupId;

    @Schema(description = "位置信息")
    private String location;

    @Schema(description = "描述信息")
    private String description;

    @Schema(description = "创建时间")
    private String createTime;
}
