package com.boonya.lab.io.iot.device;

import jakarta.annotation.PostConstruct;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class DeviceSimulator {
    @Value("${mqtt.broker: tcp://localhost:1883}")
    private String broker;

    @PostConstruct
    public void simulate() {
        for (int i = 1; i <= 100; i++) {
            String deviceId = "sensor_" + i;
            try {
                startDevice(deviceId);
            } catch (MqttException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void startDevice(String deviceId) throws MqttException {
        MqttClient client = new MqttClient(broker, deviceId);
        client.connect();

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(() -> {
            double temp = 20 + Math.random() * 15; // 20-35度
            String payload = String.format("{\"temp\": %.2f, \"ts\": %d}", temp, System.currentTimeMillis());
            MqttMessage message = new MqttMessage(payload.getBytes());
            try {
                client.publish("device/" + deviceId + "/telemetry", message);
            } catch (MqttException e) {
                throw new RuntimeException(e);
            }
        }, 0, 5, TimeUnit.SECONDS);
    }
}