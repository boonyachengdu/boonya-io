package com.boonya.lab.io.auth.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class JwtUtils {

    private final SecretKey secretKey;
    private final long expiration;
    private final long refreshExpiration;

    public JwtUtils(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expiration,
            @Value("${jwt.refresh-expiration}") long refreshExpiration) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
        this.refreshExpiration = refreshExpiration;
    }

    /**
     * 生成访问 Token（包含角色信息）
     */
    public String generateAccessToken(String userId, String username, List<String> roles) {
        return generateToken(userId, username, roles, expiration);
    }

    /**
     * 生成刷新 Token（不含角色信息）
     */
    public String generateRefreshToken(String userId, String username) {
        return generateToken(userId, username, null, refreshExpiration);
    }

    /**
     * 生成 Token
     */
    private String generateToken(String userId, String username, List<String> roles, long expirationTime) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationTime);

        var builder = Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .issuedAt(now)
                .expiration(expiryDate);

        // 角色列表放入 Token claims
        if (roles != null && !roles.isEmpty()) {
            builder.claim("roles", roles);
        }

        return builder.signWith(secretKey).compact();
    }

    /**
     * 从 Token 中获取 Claims
     */
    public Claims getClaimsFromToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.error("Failed to parse JWT token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从 Token 中获取用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? claims.getSubject() : null;
    }

    /**
     * 从 Token 中获取用户ID
     */
    public String getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? claims.get("userId", String.class) : null;
    }

    /**
     * 验证 Token 是否有效
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims != null && !isTokenExpired(claims);
        } catch (Exception e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 检查 Token 是否过期
     */
    private boolean isTokenExpired(Claims claims) {
        return claims.getExpiration().before(new Date());
    }

    /**
     * 从 Token 中获取角色列表
     */
    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims == null) {
            return List.of();
        }
        Object roles = claims.get("roles");
        if (roles instanceof List) {
            return (List<String>) roles;
        }
        return List.of();
    }

    /**
     * 刷新 Token
     */
    public String refreshToken(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims == null || isTokenExpired(claims)) {
            throw new RuntimeException("Token is expired or invalid");
        }

        String userId = claims.get("userId", String.class);
        String username = claims.getSubject();
        List<String> roles = getRolesFromToken(token);

        return generateAccessToken(userId, username, roles);
    }
}
