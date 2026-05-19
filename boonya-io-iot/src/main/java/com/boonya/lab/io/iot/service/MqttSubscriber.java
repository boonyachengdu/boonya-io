package com.boonya.lab.io.iot.service;

import com.boonya.lab.io.iot.event.OverTempEvent;
import com.boonya.lab.io.iot.utils.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MqttSubscriber {

    @Autowired
    private MqttClient mqttClient;

    @Autowired
    private TimeSeriesService timeSeriesService;

    @Autowired(required = false)
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @EventListener(ApplicationReadyEvent.class)
    public void subscribe() {
        try {
            mqttClient.subscribe("device/+/telemetry", (topic, msg) -> {
                String payload = new String(msg.getPayload());
                String deviceId = topic.split("/")[1];
                handleDeviceData(deviceId, payload);
            });
            log.info("MQTT subscriber started, listening on device/+/telemetry");
        } catch (MqttException e) {
            log.error("Failed to subscribe: {}", e.getMessage());
        }
    }

    private void handleDeviceData(String deviceId, String payload) {
        try {
            JsonNode json = JsonUtils.parse(payload);
            double temp = json.get("temp").asDouble();
            long ts = json.has("ts") ? json.get("ts").asLong() : System.currentTimeMillis();

            // 1. 存储到时序数据库
            timeSeriesService.save(deviceId, temp, ts);

            // 2. 更新Redis缓存
            if (redisTemplate != null) {
                redisTemplate.opsForValue().set("device:" + deviceId + ":latest", String.valueOf(temp));
            }

            // 3. 规则引擎：超过阈值告警
            if (temp > 30.0) {
                eventPublisher.publishEvent(new OverTempEvent(deviceId, temp, ts));
            }

            log.debug("Processed device {} data: {}℃", deviceId, temp);
        } catch (Exception e) {
            log.error("Error processing device data: {}", e.getMessage());
        }
    }
}