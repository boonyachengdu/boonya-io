package com.boonya.lab.io.iot.config;

import io.moquette.broker.Server;
import io.moquette.broker.config.IConfig;
import io.moquette.broker.config.MemoryConfig;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Slf4j
@Component
@ConditionalOnProperty(name = "mqtt.broker.type", havingValue = "embedded", matchIfMissing = true)
@RequiredArgsConstructor
public class EmbeddedMqttBroker {

    private final MqttBrokerProperties brokerProperties;
    @Getter
    private Server mqttBroker;

    @PostConstruct
    public void start() {
        try {
            Properties props = new Properties();
            props.setProperty("host", "0.0.0.0");
            props.setProperty("port", String.valueOf(brokerProperties.getEmbeddedPort()));
            props.setProperty("allow_anonymous", String.valueOf(brokerProperties.isAllowAnonymous()));

            IConfig config = new MemoryConfig(props);
            mqttBroker = new Server();
            mqttBroker.startServer(config);

            log.info("Embedded MQTT Broker started on port {}", brokerProperties.getEmbeddedPort());
        } catch (Exception e) {
            log.error("Failed to start Embedded MQTT Broker", e);
            throw new RuntimeException("Failed to start MQTT Broker", e);
        }
    }

    @PreDestroy
    public void stop() {
        if (mqttBroker != null) {
            mqttBroker.stopServer();
            log.info("Embedded MQTT Broker stopped");
        }
    }
}
