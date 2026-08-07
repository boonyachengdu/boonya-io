package com.boonya.lab.io.device.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("iot_thing_model")
@Schema(description = "物模型属性")
public class ThingModel {

    @TableId(type = IdType.AUTO)
    @Schema(description = "属性ID")
    private Long id;

    @Schema(description = "产品唯一标识")
    private String productKey;

    @Schema(description = "属性标识", example = "temperature")
    private String identifier;

    @Schema(description = "属性名称", example = "温度")
    private String name;

    @Schema(description = "数据类型：int/float/double/string/bool/enum")
    private String dataType;

    @Schema(description = "单位", example = "°C")
    private String unit;

    @Schema(description = "最小值")
    private Double minValue;

    @Schema(description = "最大值")
    private Double maxValue;

    @Schema(description = "访问模式：R-只读, W-只写, RW-读写")
    private String accessMode;

    @Schema(description = "描述信息")
    private String description;

    @Schema(description = "排序")
    private Integer sort;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
