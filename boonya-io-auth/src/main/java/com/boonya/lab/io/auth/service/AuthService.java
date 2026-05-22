package com.boonya.lab.io.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boonya.lab.io.auth.dto.LoginRequest;
import com.boonya.lab.io.auth.dto.LoginResponse;
import com.boonya.lab.io.auth.entity.User;
import com.boonya.lab.io.auth.mapper.UserMapper;
import com.boonya.lab.io.auth.util.JwtUtils;
import com.boonya.lab.io.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final JwtUtils jwtUtils;
    private final RedisTemplate<String, Object> redisTemplate;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private static final String TOKEN_BLACKLIST_KEY = "auth:token:blacklist:";

    /**
     * 用户登录
     */
    public LoginResponse login(LoginRequest request) {
        // 查询用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername());
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }

        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        // 检查用户状态
        if (!"active".equals(user.getStatus())) {
            throw new BusinessException("用户账户已被禁用");
        }

        // 生成 Token
        String userId = String.valueOf(user.getId());
        String accessToken = jwtUtils.generateAccessToken(userId, user.getUsername());
        String refreshToken = jwtUtils.generateRefreshToken(userId, user.getUsername());

        // 更新最后登录时间
        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);

        log.info("用户登录成功: {}", user.getUsername());

        // 构建响应
        LoginResponse.UserInfo userInfo = LoginResponse.UserInfo.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .email(user.getEmail())
                .build();

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(86400L)
                .userInfo(userInfo)
                .build();
    }

    /**
     * 刷新 Token
     */
    public LoginResponse refreshToken(String refreshToken) {
        // 检查 Token 是否在黑名单中
        if (isTokenBlacklisted(refreshToken)) {
            throw new BusinessException("Token 已失效");
        }

        // 验证 Refresh Token
        if (!jwtUtils.validateToken(refreshToken)) {
            throw new BusinessException("无效的 Refresh Token");
        }

        String userId = jwtUtils.getUserIdFromToken(refreshToken);
        String username = jwtUtils.getUsernameFromToken(refreshToken);

        // 生成新的 Access Token
        String newAccessToken = jwtUtils.generateAccessToken(userId, username);

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(86400L)
                .build();
    }

    /**
     * 用户登出
     */
    @Transactional
    public void logout(String accessToken) {
        // 将 Token 加入黑名单
        long expiration = jwtUtils.getClaimsFromToken(accessToken).getExpiration().getTime() - System.currentTimeMillis();
        if (expiration > 0) {
            redisTemplate.opsForValue().set(
                TOKEN_BLACKLIST_KEY + accessToken,
                "blacklisted",
                expiration,
                TimeUnit.MILLISECONDS
            );
        }

        log.info("用户登出，Token 已加入黑名单");
    }

    /**
     * 注册用户
     */
    @Transactional
    public User registerUser(String username, String password, String email, String realName) {
        // 检查用户名是否已存在
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("用户名已存在");
        }

        // 创建新用户
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setRealName(realName);
        user.setStatus("active");

        userMapper.insert(user);

        log.info("用户注册成功: {}", username);

        return user;
    }

    /**
     * 检查 Token 是否在黑名单中
     */
    private boolean isTokenBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(TOKEN_BLACKLIST_KEY + token));
    }
}
