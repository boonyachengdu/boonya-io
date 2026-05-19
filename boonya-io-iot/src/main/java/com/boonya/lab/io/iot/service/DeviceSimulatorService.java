package com.boonya.lab.io.iot.service;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class DeviceSimulatorService {

    @Value("${mqtt.broker:tcp://localhost:1883}")
    private String broker;

    @Autowired
    private MqttClient mqttClient;

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(100);
    private static final int DEVICE_COUNT = 100;

    @EventListener(ApplicationReadyEvent.class)
    public void simulate() {
        log.info("Starting device simulation with {} devices", DEVICE_COUNT);
        for (int i = 1; i <= DEVICE_COUNT; i++) {
            String deviceId = "sensor_" + i;
            startDevice(deviceId);
        }
    }

    private void startDevice(String deviceId) {
        executor.scheduleAtFixedRate(() -> {
            try {
                // 模拟温度数据：20-35度随机波动，偶尔产生异常高温
                double temp = 20 + Math.random() * 15;
                // 10%概率产生高温告警
                if (Math.random() < 0.1) {
                    temp = 30 + Math.random() * 10;
                }
                long ts = System.currentTimeMillis();
                String payload = String.format("{\"temp\": %.2f, \"ts\": %d}", temp, ts);

                MqttMessage message = new MqttMessage(payload.getBytes());
                message.setQos(1);
                mqttClient.publish("device/" + deviceId + "/telemetry", message);

                log.debug("Device {} sent temperature: {:.2f}℃", deviceId, temp);
            } catch (MqttException e) {
                log.error("Failed to send data for device {}: {}", deviceId, e.getMessage());
            }
        }, 0, 5, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }
}