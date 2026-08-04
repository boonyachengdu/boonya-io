package com.boonya.lab.io.common.config;

import com.boonya.lab.io.common.tenant.TenantInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// 修改内容：修改人：pengjunlin 时间：2026-08-04 19:00:00 -- start ----
/**
 * Web MVC 配置，注册多租户拦截器
 */
@Configuration
public class TenantWebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new TenantInterceptor());
    }
}
// 修改内容：修改人：pengjunlin 时间：2026-08-04 19:00:00 -- end ----
