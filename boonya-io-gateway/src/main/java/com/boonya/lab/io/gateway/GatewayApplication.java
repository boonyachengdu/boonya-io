package com.boonya.lab.io.gateway;

// 修改内容：修改人：pengjunlin 时间：2026-08-04 18:30:00 -- start ----
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
// 修改内容：修改人：pengjunlin 时间：2026-08-04 18:30:00 -- end ----

@SpringBootApplication
// 修改内容：修改人：pengjunlin 时间：2026-08-04 18:30:00 -- start ----
@EnableDiscoveryClient
// 修改内容：修改人：pengjunlin 时间：2026-08-04 18:30:00 -- end ----
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
