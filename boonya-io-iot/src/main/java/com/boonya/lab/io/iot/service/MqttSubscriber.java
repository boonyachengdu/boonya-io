package com.boonya.lab.io.iot.service;

import com.boonya.lab.io.iot.event.OverTempEvent;
import com.boonya.lab.io.iot.mqtt.MqttClientWrapper;
import com.boonya.lab.io.iot.utils.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MqttSubscriber {

    private final MqttClientWrapper mqttClient;
    private final TimeSeriesService timeSeriesService;
    private final ApplicationEventPublisher eventPublisher;

    @EventListener(ApplicationReadyEvent.class)
    public void subscribe() {
        try {
            mqttClient.subscribe("device/+/telemetry", (topic, payload) -> {
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

            timeSeriesService.save(deviceId, temp, ts);

            if (temp > 30.0) {
                eventPublisher.publishEvent(new OverTempEvent(deviceId, temp, ts));
            }
        } catch (Exception e) {
            log.error("Error processing device data", e);
        }
    }
}