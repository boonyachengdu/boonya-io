package com.boonya.lab.io.common.tenant;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;
/**
 * 多租户拦截器，从请求头 X-Tenant-Id 解析租户ID并写入上下文
 */
public class TenantInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String tenantId = request.getHeader("X-Tenant-Id");
        if (tenantId != null && !tenantId.isEmpty()) {
            TenantContext.setTenantId(Long.parseLong(tenantId));
        } else {
            TenantContext.setTenantId(0L); // 默认租户
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        TenantContext.clear();
    }
}
