package com.boonya.lab.io.iot.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
/**
 * 设备数据 Kafka 生产者，用于将设备数据和告警消息发送到 Kafka 主题
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceDataProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void sendDeviceData(String deviceId, String data) {
        kafkaTemplate.send("device-data", deviceId, data);
        log.debug("Sent device data to Kafka: device={}", deviceId);
    }

    public void sendAlert(String deviceId, String alertData) {
        kafkaTemplate.send("device-alerts", deviceId, alertData);
        log.debug("Sent alert to Kafka: device={}", deviceId);
    }
}
