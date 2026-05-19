package com.boonya.lab.io.iot.mqtt;

import io.moquette.broker.Server;
import io.moquette.broker.config.IConfig;
import io.moquette.broker.config.MemoryConfig;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Properties;

@Slf4j
@Component
public class EmbeddedMqttBroker {
    @PostConstruct
    public void start() {
        IConfig config = new MemoryConfig(new Properties());
        config.setProperty(NettyConfig.HOST_PROPERTY_NAME, "0.0.0.0");
        config.setProperty(NettyConfig.PORT_PROPERTY_NAME, "1883");
        Server server = new Server();
        try {
            server.startServer(config);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        log.info("Embedded MQTT Broker started on port 1883");
    }
}
