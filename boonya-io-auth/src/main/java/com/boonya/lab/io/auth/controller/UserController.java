package com.boonya.lab.io.auth.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.boonya.lab.io.auth.dto.AssignRolesRequest;
import com.boonya.lab.io.auth.dto.UserCreateRequest;
import com.boonya.lab.io.auth.dto.UserQueryRequest;
import com.boonya.lab.io.auth.dto.UserResponse;
import com.boonya.lab.io.auth.service.UserManageService;
import com.boonya.lab.io.common.response.PageResult;
import com.boonya.lab.io.common.response.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "用户管理", description = "用户查询、创建、状态管理、密码重置、角色分配")
public class UserController {

    private final UserManageService userManageService;

    @GetMapping("/query")
    @Operation(summary = "分页查询用户列表", description = "支持按用户名、真实姓名、状态筛选")
    public Result<PageResult<UserResponse>> queryUsers(UserQueryRequest request) {
        Page<UserResponse> page = userManageService.queryUsers(request);
        PageResult<UserResponse> pageResult = PageResult.of(
                page.getCurrent(), page.getSize(), page.getTotal(), page.getRecords());
        return Result.success(pageResult);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取用户详情", description = "根据ID获取用户详细信息（含角色）")
    public Result<UserResponse> getUser(@PathVariable Long id) {
        return Result.success(userManageService.getUserById(id));
    }

    @PostMapping
    @Operation(summary = "创建用户", description = "创建新用户并可选分配角色")
    public Result<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
        return Result.success(userManageService.createUser(request));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "更新用户状态", description = "更新用户状态：active-激活, inactive-未激活, locked-锁定")
    public Result<Void> updateUserStatus(@PathVariable Long id, @RequestParam String status) {
        userManageService.updateUserStatus(id, status);
        return Result.success();
    }

    @PutMapping("/{id}/password")
    @Operation(summary = "重置密码", description = "管理员重置用户密码")
    public Result<Void> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String newPassword = body.get("newPassword");
        if (newPassword == null || newPassword.length() < 6) {
            return Result.error("密码长度不能少于6位");
        }
        userManageService.resetPassword(id, newPassword);
        return Result.success();
    }

    @PutMapping("/{id}/roles")
    @Operation(summary = "分配角色", description = "为用户分配角色（覆盖原有角色）")
    public Result<Void> assignRoles(@PathVariable Long id, @RequestBody AssignRolesRequest request) {
        userManageService.assignRoles(id, request.getRoleIds());
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户", description = "逻辑删除用户")
    public Result<Void> deleteUser(@PathVariable Long id) {
        userManageService.deleteUser(id);
        return Result.success();
    }
}
