package com.boonya.lab.io.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_permission")
@Schema(description = "系统权限")
public class Permission {

    @TableId(type = IdType.AUTO)
    @Schema(description = "权限ID")
    private Long id;

    @Schema(description = "父级ID，0表示根节点")
    private Long parentId;

    @Schema(description = "权限名称", example = "设备管理")
    private String name;

    @Schema(description = "权限编码", example = "device")
    private String code;

    @Schema(description = "权限类型：MENU-菜单, BUTTON-按钮", example = "MENU")
    private String type;

    @Schema(description = "前端路由路径", example = "/devices")
    private String path;

    @Schema(description = "前端组件路径", example = "devices/index")
    private String component;

    @Schema(description = "图标", example = "Monitor")
    private String icon;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "是否启用：1-启用, 0-禁用")
    private Integer enabled;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
