package com.boonya.lab.io.iot.event.handler;

import com.boonya.lab.io.iot.event.OverTempEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

@Slf4j
@Component
public class AlertHandler {

    @Autowired
    private SimpMessagingTemplate websocket;

    @EventListener
    public void handleOverTemp(OverTempEvent event) {
        String timeStr = LocalDateTime.ofInstant(Instant.ofEpochMilli(event.getTimestamp()), ZoneId.systemDefault())
                .toString();
        String alertMsg = String.format("⚠️ 设备 %s 温度 %.1f℃ 超过阈值 (时间: %s)",
                event.getDeviceId(), event.getTemp(), timeStr);
        log.warn(alertMsg);

        // WebSocket推送到前端
        websocket.convertAndSend("/topic/alerts", Map.of(
                "message", alertMsg,
                "deviceId", event.getDeviceId(),
                "temp", event.getTemp(),
                "timestamp", event.getTimestamp()
        ));
    }
}