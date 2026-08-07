package com.boonya.lab.io.iot.feign;

import com.boonya.lab.io.common.response.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Map;

/**
 * Device 服务 Feign 客户端
 * 通过 Nacos 服务发现直接调用 boonya-io-device 服务，不经过网关，无需 JWT
 */
@FeignClient(name = "boonya-io-device", contextId = "deviceServiceClient")
public interface DeviceServiceClient {

    @GetMapping("/api/devices/by-id/{deviceId}")
    Result<Map<String, Object>> getDeviceByDeviceId(@PathVariable("deviceId") String deviceId);

    @GetMapping("/api/products/{productKey}/properties")
    Result<List<Map<String, Object>>> getThingModelProperties(@PathVariable("productKey") String productKey);

    @GetMapping("/api/products/{productKey}/services")
    Result<List<Map<String, Object>>> getThingModelServices(@PathVariable("productKey") String productKey);

    @GetMapping("/api/products/all")
    Result<List<Map<String, Object>>> listAllEnabledProducts();
}
