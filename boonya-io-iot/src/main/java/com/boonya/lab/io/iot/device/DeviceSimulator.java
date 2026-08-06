package com.boonya.lab.io.iot.device;

import com.boonya.lab.io.iot.mqtt.MqttClientWrapper;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceSimulator {

    private final MqttClientWrapper mqttClientWrapper;
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(10);
    private static final int DEVICE_COUNT = 100;

    // 能碳设备配置（电表/水表/光伏/储能），每 10s 上报一次增量值
    // value 语义：本周期增量（kWh / m³），SUM(value) 即得总量
    private static final List<EnergyDeviceConfig> ENERGY_DEVICES = List.of(
            new EnergyDeviceConfig("meter-main-001", "electricity", 5, 15),
            new EnergyDeviceConfig("meter-a2-018", "electricity", 2, 8),
            new EnergyDeviceConfig("water-main-001", "water", 0.5, 2),
            new EnergyDeviceConfig("pv-inverter-001", "solar", 8, 20),
            new EnergyDeviceConfig("ess-bms-001", "storage", 2, 6)
    );

    @EventListener(ApplicationReadyEvent.class)
    public void simulate() {
        log.info("Starting device simulation with {} temp sensors + {} energy devices",
                DEVICE_COUNT, ENERGY_DEVICES.size());
        for (int i = 1; i <= DEVICE_COUNT; i++) {
            startDevice("sensor_" + i);
        }
        ENERGY_DEVICES.forEach(this::startEnergyDevice);
    }

    private void startDevice(String deviceId) {
        long initialDelay = (long) (Math.random() * 10);
        executor.scheduleAtFixedRate(() -> {
            try {
                double temp = 20 + Math.random() * 15;
                if (Math.random() < 0.1) {
                    temp = 30 + Math.random() * 10;
                }
                long ts = System.currentTimeMillis();
                String payload = String.format("{\"temp\": %.2f, \"ts\": %d}", temp, ts);

                mqttClientWrapper.publish("device/" + deviceId + "/telemetry", payload.getBytes(), 0);
                log.debug("Device {} sent: {:.2f}℃", deviceId, temp);
            } catch (Exception e) {
                log.error("Failed to send data for device {}", deviceId, e);
            }
        }, initialDelay, 10, TimeUnit.SECONDS);
    }

    private void startEnergyDevice(EnergyDeviceConfig cfg) {
        long initialDelay = (long) (Math.random() * 5);
        executor.scheduleAtFixedRate(() -> {
            try {
                double value;
                // 光伏仅白天发电（6~18 点），夜间为 0
                if ("solar".equals(cfg.metricType())) {
                    int hour = LocalTime.now().getHour();
                    value = (hour >= 6 && hour < 18)
                            ? cfg.min() + Math.random() * (cfg.max() - cfg.min())
                            : 0;
                } else {
                    value = cfg.min() + Math.random() * (cfg.max() - cfg.min());
                }
                long ts = System.currentTimeMillis();
                String payload = String.format(
                        "{\"metricType\":\"%s\",\"value\":%.2f,\"ts\":%d}",
                        cfg.metricType(), value, ts);

                mqttClientWrapper.publish("device/" + cfg.deviceId() + "/energy", payload.getBytes(), 0);
            } catch (Exception e) {
                log.error("Failed to send energy data for device {}", cfg.deviceId(), e);
            }
        }, initialDelay, 10, TimeUnit.SECONDS);
    }

    private record EnergyDeviceConfig(String deviceId, String metricType, double min, double max) {}

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }
}
