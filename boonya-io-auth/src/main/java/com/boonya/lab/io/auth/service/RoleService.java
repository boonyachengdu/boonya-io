package com.boonya.lab.io.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.boonya.lab.io.auth.dto.RoleCreateRequest;
import com.boonya.lab.io.auth.dto.RoleResponse;
import com.boonya.lab.io.auth.entity.Role;
import com.boonya.lab.io.auth.mapper.RoleMapper;
import com.boonya.lab.io.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleMapper roleMapper;

    /**
     * 查询所有角色（用于下拉选择）
     */
    public List<RoleResponse> listAllRoles() {
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Role::getId);
        return roleMapper.selectList(wrapper).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 分页查询角色
     */
    public Page<RoleResponse> queryRoles(int page, int size, String roleName) {
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        if (roleName != null && !roleName.isBlank()) {
            wrapper.like(Role::getRoleName, roleName);
        }
        wrapper.orderByAsc(Role::getId);

        Page<Role> rolePage = roleMapper.selectPage(new Page<>(page, size), wrapper);

        Page<RoleResponse> resultPage = new Page<>(rolePage.getCurrent(), rolePage.getSize(), rolePage.getTotal());
        resultPage.setRecords(rolePage.getRecords().stream()
                .map(this::toResponse)
                .collect(Collectors.toList()));
        return resultPage;
    }

    /**
     * 根据ID列表查询角色
     */
    public List<RoleResponse> listRolesByIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return List.of();
        }
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Role::getId, roleIds);
        return roleMapper.selectList(wrapper).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 创建角色
     */
    @Transactional
    public RoleResponse createRole(RoleCreateRequest request) {
        // 检查角色编码是否已存在
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Role::getRoleCode, request.getRoleCode());
        if (roleMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("角色编码已存在");
        }

        Role role = new Role();
        role.setRoleName(request.getRoleName());
        role.setRoleCode(request.getRoleCode());
        role.setDescription(request.getDescription());

        roleMapper.insert(role);
        log.info("角色创建成功: {}", role.getRoleCode());

        return toResponse(role);
    }

    /**
     * 更新角色
     */
    @Transactional
    public RoleResponse updateRole(Long id, RoleCreateRequest request) {
        Role role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }

        // 检查角色编码是否与其他角色冲突
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Role::getRoleCode, request.getRoleCode()).ne(Role::getId, id);
        if (roleMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("角色编码已存在");
        }

        role.setRoleName(request.getRoleName());
        role.setRoleCode(request.getRoleCode());
        role.setDescription(request.getDescription());

        roleMapper.updateById(role);
        log.info("角色更新成功: {}", role.getRoleCode());

        return toResponse(role);
    }

    /**
     * 删除角色
     */
    @Transactional
    public void deleteRole(Long id) {
        Role role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        roleMapper.deleteById(id);
        log.info("角色删除成功: {}", role.getRoleCode());
    }

    private RoleResponse toResponse(Role role) {
        return RoleResponse.builder()
                .id(role.getId())
                .roleName(role.getRoleName())
                .roleCode(role.getRoleCode())
                .description(role.getDescription())
                .createTime(role.getCreateTime())
                .build();
    }
}
