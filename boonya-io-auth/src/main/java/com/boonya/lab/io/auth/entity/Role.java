package com.boonya.lab.io.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_role")
@Schema(description = "系统角色")
public class Role {

    @TableId(type = IdType.AUTO)
    @Schema(description = "角色ID")
    private Long id;

    @Schema(description = "角色名称", example = "超级管理员")
    private String roleName;

    @Schema(description = "角色编码", example = "ROLE_ADMIN")
    private String roleCode;

    @Schema(description = "角色描述")
    private String description;

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
