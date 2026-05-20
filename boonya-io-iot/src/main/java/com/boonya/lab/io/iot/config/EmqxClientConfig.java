package com.boonya.lab.io.iot.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "mqtt.broker.type", havingValue = "emqx")
@RequiredArgsConstructor
public class EmqxClientConfig {

    private final MqttBrokerProperties brokerProperties;

    @Bean
    public MqttClient mqttClient() throws MqttException {
        String clientId = "spring-boot-" + System.currentTimeMillis();
        MqttClient client = new MqttClient(brokerProperties.getHost(), clientId, new MemoryPersistence());

        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        options.setConnectionTimeout(10);
        options.setKeepAliveInterval(60);

        client.connect(options);
        log.info("Connected to external EMQX at {}", brokerProperties.getHost());
        return client;
    }
}