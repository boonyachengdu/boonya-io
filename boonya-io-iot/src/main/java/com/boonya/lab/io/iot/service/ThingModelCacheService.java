package com.boonya.lab.io.iot.service;

import com.boonya.lab.io.iot.feign.DeviceServiceClient;
import com.boonya.lab.io.iot.utils.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 物模型缓存服务
 * 通过 Feign 调用 device 服务获取物模型定义，缓存到 Redis 减少 RPC 调用
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ThingModelCacheService {

    private final DeviceServiceClient deviceServiceClient;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String DEVICE_PRODUCT_KEY_CACHE = "iot:device:product-key:";
    private static final String THING_MODEL_PROPERTIES_CACHE = "iot:thing-model:properties:";
    private static final long PRODUCT_KEY_TTL_MINUTES = 30;
    private static final long PROPERTIES_TTL_MINUTES = 5;

    /**
     * 获取设备的 productKey（带缓存）
     */
    @SuppressWarnings("unchecked")
    public String getDeviceProductKey(String deviceId) {
        String cacheKey = DEVICE_PRODUCT_KEY_CACHE + deviceId;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached.toString();
        }

        try {
            var result = deviceServiceClient.getDeviceByDeviceId(deviceId);
            if (result != null && result.isSuccess() && result.getData() != null) {
                Map<String, Object> device = result.getData();
                String productKey = (String) device.get("productKey");
                if (productKey != null && !productKey.isEmpty()) {
                    redisTemplate.opsForValue().set(cacheKey, productKey, PRODUCT_KEY_TTL_MINUTES, TimeUnit.MINUTES);
                    return productKey;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to get productKey for device {}: {}", deviceId, e.getMessage());
        }
        return null;
    }

    /**
     * 获取物模型属性列表（带缓存）
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getProperties(String productKey) {
        if (productKey == null || productKey.isEmpty()) {
            return Collections.emptyList();
        }

        String cacheKey = THING_MODEL_PROPERTIES_CACHE + productKey;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof List) {
            return (List<Map<String, Object>>) cached;
        }

        try {
            var result = deviceServiceClient.getThingModelProperties(productKey);
            if (result != null && result.isSuccess() && result.getData() != null) {
                List<Map<String, Object>> properties = result.getData();
                redisTemplate.opsForValue().set(cacheKey, properties, PROPERTIES_TTL_MINUTES, TimeUnit.MINUTES);
                return properties;
            }
        } catch (Exception e) {
            log.warn("Failed to get thing model properties for product {}: {}", productKey, e.getMessage());
        }
        return Collections.emptyList();
    }

    /**
     * 根据物模型解析 payload 为属性 Map
     * 如果物模型不存在或 productKey 为空，降级为硬编码 temp 解析
     *
     * @param productKey 产品Key
     * @param payload    JSON payload
     * @return 属性 Map: {identifier -> value}
     */
    public Map<String, Object> parsePayload(String productKey, String payload) {
        Map<String, Object> result = new HashMap<>();
        try {
            JsonNode json = JsonUtils.parse(payload);
            long ts = json.has("ts") ? json.get("ts").asLong() : System.currentTimeMillis();
            result.put("ts", ts);

            List<Map<String, Object>> properties = getProperties(productKey);
            if (properties == null || properties.isEmpty()) {
                // 降级：物模型不存在，尝试解析 temp 字段（兼容历史温度传感器）
                if (json.has("temp")) {
                    result.put("temp", json.get("temp").asDouble());
                }
                // 同时保留 payload 中所有已知字段
                json.fields().forEachRemaining(entry -> {
                    if (!"ts".equals(entry.getKey())) {
                        result.putIfAbsent(entry.getKey(), entry.getValue().asText());
                    }
                });
                return result;
            }

            // 按物模型属性定义解析
            for (Map<String, Object> prop : properties) {
                String identifier = (String) prop.get("identifier");
                String dataType = (String) prop.getOrDefault("dataType", "string");
                if (identifier != null && json.has(identifier)) {
                    JsonNode valueNode = json.get(identifier);
                    Object value = parseValueByType(valueNode, dataType);
                    result.put(identifier, value);
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse payload for product {}: {}", productKey, e.getMessage());
        }
        return result;
    }

    private Object parseValueByType(JsonNode valueNode, String dataType) {
        if (valueNode == null || valueNode.isNull()) {
            return null;
        }
        return switch (dataType) {
            case "int", "long" -> valueNode.asLong();
            case "float", "double" -> valueNode.asDouble();
            case "bool" -> valueNode.asBoolean();
            default -> valueNode.asText();
        };
    }
}
