package com.boonya.lab.io.iot.service;

import com.boonya.lab.io.iot.mqtt.MqttClientWrapper;
import com.boonya.lab.io.iot.ruleengine.RuleEngine;
import com.boonya.lab.io.iot.utils.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MqttSubscriber {

    private final MqttClientWrapper mqttClient;
    private final TimeSeriesService timeSeriesService;
    private final RuleEngine ruleEngine;
    private final ThingModelCacheService thingModelCacheService;

    @EventListener(ApplicationReadyEvent.class)
    public void subscribe() {
        try {
            ruleEngine.initDefaultRules();

            // 通用设备遥测（基于物模型动态解析）
            mqttClient.subscribe("device/+/telemetry", (topic, payload) -> {
                String deviceId = topic.split("/")[1];
                String message = new String(payload);
                handleDeviceData(deviceId, message);
            });

            // 能碳设备指标（电表/水表/光伏/储能）
            mqttClient.subscribe("device/+/energy", (topic, payload) -> {
                String deviceId = topic.split("/")[1];
                handleEnergyData(deviceId, new String(payload));
            });

            log.info("MQTT subscriber started (telemetry + energy)");
        } catch (Exception e) {
            log.error("Failed to subscribe: {}", e.getMessage());
        }
    }

    private void handleDeviceData(String deviceId, String payload) {
        try {
            // 通过物模型缓存服务获取 productKey 并解析 payload
            String productKey = thingModelCacheService.getDeviceProductKey(deviceId);
            Map<String, Object> data = thingModelCacheService.parsePayload(productKey, payload);

            long ts = data.containsKey("ts") ? ((Number) data.get("ts")).longValue() : System.currentTimeMillis();

            // 存储到时序数据库：遍历所有数值属性写入 device_properties 超表
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                String identifier = entry.getKey();
                if ("ts".equals(identifier)) continue;
                Object value = entry.getValue();
                if (value instanceof Number) {
                    timeSeriesService.saveProperty(deviceId, identifier, ((Number) value).doubleValue(), ts);
                }
            }

            // 兼容历史：如果有 temp 属性，同时写入 iot.devices 超表（保留原有温度趋势查询）
            if (data.containsKey("temp") && data.get("temp") instanceof Number) {
                double temp = ((Number) data.get("temp")).doubleValue();
                timeSeriesService.save(deviceId, temp, ts);
            }

            // 使用规则引擎评估（传入完整属性 Map）
            ruleEngine.evaluate(deviceId, data);

            // 预留：设备影子 reported 更新（Phase 2 实现）
            // deviceShadowService.updateReported(deviceId, data);

        } catch (Exception e) {
            log.error("Error processing device data for {}: {}", deviceId, e.getMessage());
        }
    }

    private void handleEnergyData(String deviceId, String payload) {
        try {
            JsonNode json = JsonUtils.parse(payload);
            String metricType = json.has("metricType") ? json.get("metricType").asText() : "electricity";
            double value = json.get("value").asDouble();
            long ts = json.has("ts") ? json.get("ts").asLong() : System.currentTimeMillis();

            // 存储到能碳超表
            timeSeriesService.saveEnergyMetric(deviceId, metricType, value, ts);

            log.debug("Energy metric saved: device={}, type={}, value={}", deviceId, metricType, value);
        } catch (Exception e) {
            log.error("Error processing energy data", e);
        }
    }
}