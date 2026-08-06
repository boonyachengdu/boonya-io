package com.boonya.lab.io.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "角色创建/更新请求")
public class RoleCreateRequest {

    @NotBlank(message = "角色名称不能为空")
    @Schema(description = "角色名称", example = "超级管理员")
    private String roleName;

    @NotBlank(message = "角色编码不能为空")
    @Schema(description = "角色编码", example = "ROLE_ADMIN")
    private String roleCode;

    @Schema(description = "角色描述")
    private String description;
}
