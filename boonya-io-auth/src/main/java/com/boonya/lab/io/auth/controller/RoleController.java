package com.boonya.lab.io.auth.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.boonya.lab.io.auth.dto.RoleCreateRequest;
import com.boonya.lab.io.auth.dto.RoleResponse;
import com.boonya.lab.io.auth.service.RoleService;
import com.boonya.lab.io.common.response.PageResult;
import com.boonya.lab.io.common.response.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
@Tag(name = "角色管理", description = "角色查询、创建、更新、删除")
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @Operation(summary = "查询所有角色", description = "获取所有角色列表（用于下拉选择）")
    public Result<List<RoleResponse>> listAllRoles() {
        return Result.success(roleService.listAllRoles());
    }

    @GetMapping("/query")
    @Operation(summary = "分页查询角色", description = "分页查询角色列表，支持按角色名称筛选")
    public Result<PageResult<RoleResponse>> queryRoles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String roleName) {
        Page<RoleResponse> rolePage = roleService.queryRoles(page, size, roleName);
        PageResult<RoleResponse> pageResult = PageResult.of(
                rolePage.getCurrent(), rolePage.getSize(), rolePage.getTotal(), rolePage.getRecords());
        return Result.success(pageResult);
    }

    @PostMapping
    @Operation(summary = "创建角色", description = "创建新角色")
    public Result<RoleResponse> createRole(@Valid @RequestBody RoleCreateRequest request) {
        return Result.success(roleService.createRole(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新角色", description = "更新角色信息")
    public Result<RoleResponse> updateRole(@PathVariable Long id, @Valid @RequestBody RoleCreateRequest request) {
        return Result.success(roleService.updateRole(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除角色", description = "逻辑删除角色")
    public Result<Void> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return Result.success();
    }
}
