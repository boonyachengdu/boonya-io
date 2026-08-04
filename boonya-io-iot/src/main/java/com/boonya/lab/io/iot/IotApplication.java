package com.boonya.lab.io.iot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
// 修改内容：修改人：pengjunlin 时间：2026-08-04 18:30:00 -- start ----
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
// 修改内容：修改人：pengjunlin 时间：2026-08-04 18:30:00 -- end ----

@SpringBootApplication
// 修改内容：修改人：pengjunlin 时间：2026-08-04 18:30:00 -- start ----
@EnableDiscoveryClient           // Nacos 注册
@EnableFeignClients              // 开启 Feign
// 修改内容：修改人：pengjunlin 时间：2026-08-04 18:30:00 -- end ----
public class IotApplication {
    public static void main(String[] args) {
        SpringApplication.run(IotApplication.class, args);
    }
}
