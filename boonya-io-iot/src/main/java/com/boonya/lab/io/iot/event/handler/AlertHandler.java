package com.boonya.lab.io.iot.event.handler;

import com.boonya.lab.io.iot.event.OverTempEvent;
import com.boonya.lab.io.iot.mqtt.MqttClientWrapper;
import com.boonya.lab.io.iot.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlertHandler {

    private final SimpMessagingTemplate websocket;
    private final MqttClientWrapper mqttClient;

    @EventListener
    public void handleOverTemp(OverTempEvent event) {
        String timeStr = LocalDateTime.ofInstant(Instant.ofEpochMilli(event.getTimestamp()), ZoneId.systemDefault())
                .toString();
        String alertMsg = String.format("⚠️ 设备 %s 温度 %.1f℃ 超过阈值 (时间: %s)",
                event.getDeviceId(), event.getTemp(), timeStr);
        log.warn(alertMsg);

        Map<String, Object> alert = Map.of(
                "message", alertMsg,
                "deviceId", event.getDeviceId(),
                "temp", event.getTemp(),
                "timestamp", event.getTimestamp()
        );

        // WebSocket（STOMP）推送到前端
        websocket.convertAndSend("/topic/alerts", alert);

        // MQTT 推送到 alerts/{deviceId}，供前端 MQTT.js 直连订阅（实时告警铃铛/告警列表）
        try {
            mqttClient.publish("alerts/" + event.getDeviceId(),
                    JsonUtils.toJson(alert).getBytes(StandardCharsets.UTF_8), 1);
        } catch (Exception e) {
            log.error("Failed to publish alert via MQTT: {}", e.getMessage());
        }
    }
}