package com.boonya.lab.io.iot.device;

import com.boonya.lab.io.iot.mqtt.MqttClientWrapper;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceSimulator {

    private final MqttClientWrapper mqttClientWrapper;
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
                double temp = 20 + Math.random() * 15;
                if (Math.random() < 0.1) {
                    temp = 30 + Math.random() * 10;
                }
                long ts = System.currentTimeMillis();
                String payload = String.format("{\"temp\": %.2f, \"ts\": %d}", temp, ts);

                mqttClientWrapper.publish("device/" + deviceId + "/telemetry", payload.getBytes(), 1);
                log.debug("Device {} sent: {:.2f}℃", deviceId, temp);
            } catch (Exception e) {
                log.error("Failed to send data for device {}", deviceId, e);
            }
        }, 0, 5, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }
}
