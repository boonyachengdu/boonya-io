package com.boonya.lab.io.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;
/**
 * 网关限流配置
 * 基于 Redis 令牌桶算法实现 IP 维度的请求限流
 */
@Configuration
public class RateLimiterConfig {

    /**
     * 基于客户端 IP 的限流 Key 解析器
     */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just(
            exchange.getRequest().getRemoteAddress() != null
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                : "unknown"
        );
    }
}
