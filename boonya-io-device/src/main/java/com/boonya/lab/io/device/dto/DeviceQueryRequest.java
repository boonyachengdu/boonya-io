package com.boonya.lab.io.device.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "设备查询参数")
public class DeviceQueryRequest {

    @Schema(description = "设备ID（模糊搜索）")
    private String deviceId;

    @Schema(description = "设备名称（模糊搜索）")
    private String deviceName;

    @Schema(description = "设备类型")
    private String deviceType;

    @Schema(description = "设备状态")
    private String status;

    @Schema(description = "分组ID")
    private Long groupId;

    @Schema(description = "页码", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页数量", example = "10")
    private Integer pageSize = 10;
}
