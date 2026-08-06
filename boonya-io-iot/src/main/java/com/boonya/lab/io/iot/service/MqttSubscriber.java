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

    @EventListener(ApplicationReadyEvent.class)
    public void subscribe() {
        try {
            ruleEngine.initDefaultRules();

            // 通用设备遥测（温度传感器）
            mqttClient.subscribe("device/+/telemetry", (topic, payload) -> {
                log.info("Received message - Topic: {}, Payload: {}", topic, new String(payload));
                String deviceId = topic.split("/")[1];
                String message = new String(payload);
                handleDeviceData(deviceId, message);
            });

            // 能碳设备指标（电表/水表/光伏/储能）
            // 主题：device/{deviceId}/energy，payload: {"metricType":"electricity","value":123.45,"ts":...}
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
            JsonNode json = JsonUtils.parse(payload);
            double temp = json.get("temp").asDouble();
            long ts = json.has("ts") ? json.get("ts").asLong() : System.currentTimeMillis();

            // 存储到时序数据库
            timeSeriesService.save(deviceId, temp, ts);

            // 使用规则引擎评估
            Map<String, Object> data = new HashMap<>();
            data.put("temp", temp);
            data.put("ts", ts);
            ruleEngine.evaluate(deviceId, data);

        } catch (Exception e) {
            log.error("Error processing device data", e);
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