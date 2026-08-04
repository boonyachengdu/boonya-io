package com.boonya.lab.io.common.tenant;

// 修改内容：修改人：pengjunlin 时间：2026-08-04 19:00:00 -- start ----
/**
 * 多租户上下文，基于 ThreadLocal 保存当前请求的租户ID
 */
public class TenantContext {
    private static final ThreadLocal<Long> CURRENT_TENANT = new ThreadLocal<>();

    public static void setTenantId(Long tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static Long getTenantId() {
        return CURRENT_TENANT.get();
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
// 修改内容：修改人：pengjunlin 时间：2026-08-04 19:00:00 -- end ----
