package com.boonya.lab.io.device.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("iot_thing_service")
@Schema(description = "物模型服务（设备可被调用的功能）")
public class ThingService {

    @TableId(type = IdType.AUTO)
    @Schema(description = "服务ID")
    private Long id;

    @Schema(description = "产品唯一标识")
    private String productKey;

    @Schema(description = "服务标识", example = "set_temperature")
    private String identifier;

    @Schema(description = "服务名称", example = "设置温度")
    private String name;

    @Schema(description = "调用方式：SYNC-同步, ASYNC-异步")
    private String callType;

    @Schema(description = "输入参数定义（JSON）")
    private String inputParams;

    @Schema(description = "输出参数定义（JSON）")
    private String outputParams;

    @Schema(description = "描述信息")
    private String description;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
