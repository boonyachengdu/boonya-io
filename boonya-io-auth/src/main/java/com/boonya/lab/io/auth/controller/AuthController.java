package com.boonya.lab.io.auth.controller;

import com.boonya.lab.io.auth.dto.LoginRequest;
import com.boonya.lab.io.auth.dto.LoginResponse;
import com.boonya.lab.io.auth.service.AuthService;
import com.boonya.lab.io.common.response.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "认证授权", description = "用户登录、注册、Token 管理")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "使用用户名和密码登录，返回 JWT Token")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return Result.success("登录成功", response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "刷新 Token", description = "使用 Refresh Token 获取新的 Access Token")
    public Result<LoginResponse> refreshToken(@RequestParam String refreshToken) {
        LoginResponse response = authService.refreshToken(refreshToken);
        return Result.success("Token 刷新成功", response);
    }

    @PostMapping("/logout")
    @Operation(summary = "用户登出", description = "将 Access Token 加入黑名单")
    public Result<Void> logout(@RequestParam String accessToken) {
        authService.logout(accessToken);
        return Result.success("登出成功", null);
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "注册新用户")
    public Result<Void> register(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String email,
            @RequestParam(required = false) String realName) {
        authService.registerUser(username, password, email, realName);
        return Result.success("注册成功", null);
    }
}
