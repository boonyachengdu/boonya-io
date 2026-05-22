package com.boonya.lab.io.device.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("device_group")
@Schema(description = "设备分组")
public class DeviceGroup {

    @TableId(type = IdType.AUTO)
    @Schema(description = "分组ID")
    private Long id;

    @Schema(description = "分组名称", example = "生产车间A")
    private String groupName;

    @Schema(description = "分组编码", example = "workshop_a")
    private String groupCode;

    @Schema(description = "父分组ID")
    private Long parentId;

    @Schema(description = "描述")
    private String description;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}

