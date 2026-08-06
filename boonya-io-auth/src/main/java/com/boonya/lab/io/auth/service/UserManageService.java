package com.boonya.lab.io.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.boonya.lab.io.auth.dto.UserCreateRequest;
import com.boonya.lab.io.auth.dto.UserQueryRequest;
import com.boonya.lab.io.auth.dto.UserResponse;
import com.boonya.lab.io.auth.entity.User;
import com.boonya.lab.io.auth.entity.UserRole;
import com.boonya.lab.io.auth.mapper.UserMapper;
import com.boonya.lab.io.auth.mapper.UserRoleMapper;
import com.boonya.lab.io.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserManageService {

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleService roleService;
    private final PasswordEncoder passwordEncoder;

    /**
     * 分页查询用户列表
     */
    public Page<UserResponse> queryUsers(UserQueryRequest request) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            wrapper.like(User::getUsername, request.getUsername());
        }
        if (request.getRealName() != null && !request.getRealName().isBlank()) {
            wrapper.like(User::getRealName, request.getRealName());
        }
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            wrapper.eq(User::getStatus, request.getStatus());
        }
        wrapper.orderByDesc(User::getCreateTime);

        Page<User> userPage = userMapper.selectPage(
                new Page<>(request.getPage(), request.getSize()), wrapper);

        Page<UserResponse> resultPage = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        resultPage.setRecords(userPage.getRecords().stream()
                .map(this::toResponse)
                .collect(Collectors.toList()));
        return resultPage;
    }

    /**
     * 根据ID获取用户详情
     */
    public UserResponse getUserById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return toResponse(user);
    }

    /**
     * 创建用户
     */
    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        // 检查用户名是否已存在
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername());
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("用户名已存在");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setRealName(request.getRealName());
        user.setStatus("active");

        userMapper.insert(user);

        // 分配角色
        if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
            assignRoles(user.getId(), request.getRoleIds());
        }

        log.info("用户创建成功: {}", user.getUsername());
        return toResponse(user);
    }

    /**
     * 更新用户状态（启用/禁用/锁定）
     */
    @Transactional
    public void updateUserStatus(Long id, String status) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setStatus(status);
        userMapper.updateById(user);
        log.info("用户状态更新: {} -> {}", user.getUsername(), status);
    }

    /**
     * 重置密码
     */
    @Transactional
    public void resetPassword(Long id, String newPassword) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
        log.info("用户密码重置: {}", user.getUsername());
    }

    /**
     * 分配角色
     */
    @Transactional
    public void assignRoles(Long userId, List<Long> roleIds) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 先删除原有角色关联
        LambdaQueryWrapper<UserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserRole::getUserId, userId);
        userRoleMapper.delete(wrapper);

        // 再插入新的角色关联
        if (roleIds != null) {
            for (Long roleId : roleIds) {
                UserRole userRole = new UserRole();
                userRole.setUserId(userId);
                userRole.setRoleId(roleId);
                userRoleMapper.insert(userRole);
            }
        }
        log.info("用户角色分配: {} -> {}", user.getUsername(), roleIds);
    }

    /**
     * 删除用户（逻辑删除）
     */
    @Transactional
    public void deleteUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        userMapper.deleteById(id);

        // 同时删除角色关联
        LambdaQueryWrapper<UserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserRole::getUserId, id);
        userRoleMapper.delete(wrapper);

        log.info("用户删除: {}", user.getUsername());
    }

    /**
     * 实体转响应DTO（含角色信息）
     */
    private UserResponse toResponse(User user) {
        List<Long> roleIds = userRoleMapper.selectRoleIdsByUserId(user.getId());
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phone(user.getPhone())
                .realName(user.getRealName())
                .status(user.getStatus())
                .lastLoginTime(user.getLastLoginTime())
                .createTime(user.getCreateTime())
                .roles(roleService.listRolesByIds(roleIds))
                .build();
    }
}
