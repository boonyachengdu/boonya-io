package com.boonya.lab.io.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_role_permission")
@Schema(description = "角色权限关联")
public class RolePermission {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "角色ID")
    private Long roleId;

    @Schema(description = "权限ID")
    private Long permissionId;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
