package com.boonya.lab.io.iot.event.handler;

import com.boonya.lab.io.iot.event.OverTempEvent;
import com.boonya.lab.io.iot.mqtt.MqttClientWrapper;
import com.boonya.lab.io.iot.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
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

    /**
     * PostgreSQL JdbcTemplate（连接 iot_device 库，用于告警持久化到 device_alert 表）。
     * 与 TimeSeriesService 注入 TDengine JdbcTemplate 的方式一致，采用 required=false：
     * 未配置 pg.datasource.url 时不装配，告警仍通过 WebSocket / MQTT 推送。
     */
    @Autowired(required = false)
    @Qualifier("pgJdbcTemplate")
    private JdbcTemplate pgJdbcTemplate;

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

        // 持久化到 PostgreSQL device_alert 表（与推送逻辑独立，失败不影响告警推送）
        persistAlert(event, alertMsg);
    }

    /**
     * 将告警写入 device_alert 表。
     * OverTempEvent 不携带阈值字段，threshold 列写入 NULL。
     */
    private void persistAlert(OverTempEvent event, String alertMsg) {
        if (pgJdbcTemplate == null) {
            return;
        }
        String sql = "INSERT INTO device_alert (device_id, alert_type, severity, title, message, " +
                "metric_value, threshold, status, trigger_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, 'PENDING', NOW())";
        try {
            pgJdbcTemplate.update(sql,
                    event.getDeviceId(),
                    "OVER_TEMP",
                    "WARNING",
                    "温度过高告警",
                    alertMsg,
                    event.getTemp(),
                    null);
        } catch (Exception e) {
            log.error("Failed to persist alert to device_alert: {}", e.getMessage());
        }
    }
}