package com.boonya.lab.io.auth.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boonya.lab.io.auth.entity.Permission;
import com.boonya.lab.io.auth.entity.RolePermission;
import com.boonya.lab.io.auth.mapper.PermissionMapper;
import com.boonya.lab.io.auth.mapper.RolePermissionMapper;
import com.boonya.lab.io.common.exception.BusinessException;
import com.boonya.lab.io.common.response.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "权限管理", description = "权限查询、权限树、角色权限分配")
public class PermissionController {

    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;

    /**
     * 权限树节点（在权限基础上扩展 children 字段）
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class PermissionNode extends Permission {
        private List<PermissionNode> children = new ArrayList<>();
    }

    @GetMapping("/api/permissions/tree")
    @Operation(summary = "获取权限树", description = "按 parentId 构建树形结构")
    public Result<List<PermissionNode>> permissionTree() {
        List<Permission> permissions = permissionMapper.selectList(
                new LambdaQueryWrapper<Permission>().orderByAsc(Permission::getSort));
        return Result.success(buildTree(permissions));
    }

    @GetMapping("/api/permissions")
    @Operation(summary = "权限列表", description = "获取全部权限列表")
    public Result<List<Permission>> listPermissions() {
        List<Permission> permissions = permissionMapper.selectList(
                new LambdaQueryWrapper<Permission>().orderByAsc(Permission::getSort));
        return Result.success(permissions);
    }

    @PostMapping("/api/permissions")
    @Operation(summary = "创建权限", description = "创建新权限")
    public Result<Permission> createPermission(@RequestBody Permission permission) {
        // 校验权限编码唯一
        LambdaQueryWrapper<Permission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Permission::getCode, permission.getCode());
        if (permissionMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("权限编码已存在");
        }
        permission.setId(null);
        permissionMapper.insert(permission);
        log.info("权限创建成功: {}", permission.getCode());
        return Result.success(permission);
    }

    @PutMapping("/api/permissions/{id}")
    @Operation(summary = "更新权限", description = "更新权限信息")
    public Result<Permission> updatePermission(@PathVariable Long id, @RequestBody Permission permission) {
        Permission existing = permissionMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("权限不存在");
        }
        // 校验权限编码不与其他权限冲突
        LambdaQueryWrapper<Permission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Permission::getCode, permission.getCode()).ne(Permission::getId, id);
        if (permissionMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("权限编码已存在");
        }
        permission.setId(id);
        permissionMapper.updateById(permission);
        log.info("权限更新成功: {}", permission.getCode());
        return Result.success(permissionMapper.selectById(id));
    }

    @DeleteMapping("/api/permissions/{id}")
    @Operation(summary = "删除权限", description = "删除权限及其角色关联")
    @Transactional
    public Result<Void> deletePermission(@PathVariable Long id) {
        Permission existing = permissionMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("权限不存在");
        }
        // 清理角色权限关联
        rolePermissionMapper.delete(new LambdaQueryWrapper<RolePermission>()
                .eq(RolePermission::getPermissionId, id));
        permissionMapper.deleteById(id);
        log.info("权限删除成功: {}", existing.getCode());
        return Result.success();
    }

    @GetMapping("/api/roles/{id}/permissions")
    @Operation(summary = "获取角色已分配的权限ID列表", description = "返回角色关联的权限ID集合")
    public Result<List<Long>> listRolePermissions(@PathVariable Long id) {
        List<RolePermission> list = rolePermissionMapper.selectList(
                new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, id));
        List<Long> permissionIds = list.stream()
                .map(RolePermission::getPermissionId)
                .collect(Collectors.toList());
        return Result.success(permissionIds);
    }

    @PutMapping("/api/roles/{id}/permissions")
    @Operation(summary = "分配角色权限", description = "覆盖式分配角色权限，接收 permissionIds 数组")
    @Transactional
    public Result<Void> assignRolePermissions(@PathVariable Long id, @RequestBody List<Long> permissionIds) {
        // 删除原有关联
        rolePermissionMapper.delete(new LambdaQueryWrapper<RolePermission>()
                .eq(RolePermission::getRoleId, id));

        // 批量插入新关联
        if (permissionIds != null && !permissionIds.isEmpty()) {
            for (Long permissionId : permissionIds) {
                RolePermission rp = new RolePermission();
                rp.setRoleId(id);
                rp.setPermissionId(permissionId);
                rolePermissionMapper.insert(rp);
            }
        }
        log.info("角色权限分配成功: roleId={}, permissionCount={}", id,
                permissionIds == null ? 0 : permissionIds.size());
        return Result.success();
    }

    /**
     * 构建权限树
     */
    private List<PermissionNode> buildTree(List<Permission> permissions) {
        Map<Long, PermissionNode> nodeMap = new LinkedHashMap<>();
        for (Permission p : permissions) {
            PermissionNode node = new PermissionNode();
            BeanUtils.copyProperties(p, node);
            node.setChildren(new ArrayList<>());
            nodeMap.put(node.getId(), node);
        }

        List<PermissionNode> roots = new ArrayList<>();
        for (PermissionNode node : nodeMap.values()) {
            Long parentId = node.getParentId();
            if (parentId == null || parentId == 0L) {
                roots.add(node);
            } else {
                PermissionNode parent = nodeMap.get(parentId);
                if (parent != null) {
                    parent.getChildren().add(node);
                } else {
                    // 父节点不存在则作为根节点
                    roots.add(node);
                }
            }
        }
        return roots;
    }
}
