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
            // 初始化默认规则
            ruleEngine.initDefaultRules();

            mqttClient.subscribe("device/+/telemetry", (topic, payload) -> {
                log.info("Received message - Topic: {}, Payload: {}", topic, new String(payload));
                String deviceId = topic.split("/")[1];
                String message = new String(payload);
                handleDeviceData(deviceId, message);
            });
            log.info("MQTT subscriber started");
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
}