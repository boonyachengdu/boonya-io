package com.boonya.lab.io.ota;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class OtaApplication {

    public static void main(String[] args) {
        SpringApplication.run(OtaApplication.class, args);
    }
}
