package com.boonya.lab.io.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "分配角色请求")
public class AssignRolesRequest {

    @Schema(description = "角色ID列表")
    private List<Long> roleIds;
}
