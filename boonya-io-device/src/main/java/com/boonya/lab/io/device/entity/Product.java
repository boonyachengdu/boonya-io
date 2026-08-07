package com.boonya.lab.io.device.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("iot_product")
@Schema(description = "产品（设备模板）")
public class Product {

    @TableId(type = IdType.AUTO)
    @Schema(description = "产品ID")
    private Long id;

    @Schema(description = "产品唯一标识", example = "temp_sensor_v1")
    private String productKey;

    @Schema(description = "产品名称", example = "温度传感器V1")
    private String productName;

    @Schema(description = "节点类型：DIRECT-直连设备, GATEWAY-网关, SUBDEVICE-子设备")
    private String nodeType;

    @Schema(description = "协议类型：MQTT/COAP/HTTP")
    private String protocolType;

    @Schema(description = "数据格式：JSON/CUSTOM")
    private String dataFormat;

    @Schema(description = "描述信息")
    private String description;

    @Schema(description = "是否启用")
    private Boolean enabled;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
