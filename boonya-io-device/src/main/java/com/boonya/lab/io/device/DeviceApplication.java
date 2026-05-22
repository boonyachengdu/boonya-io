package com.boonya.lab.io.device;

import com.boonya.lab.io.cache.redis.config.RedisTemplateConfig;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@Import({
        RedisTemplateConfig.class
})
@SpringBootApplication
@MapperScan("com.boonya.lab.io.device.mapper")
public class DeviceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeviceApplication.class, args);
    }
}
