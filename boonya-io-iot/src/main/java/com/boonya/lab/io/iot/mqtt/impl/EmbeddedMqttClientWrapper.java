package com.boonya.lab.io.iot.mqtt.impl;


import com.boonya.lab.io.iot.config.EmbeddedMqttBroker;
import com.boonya.lab.io.iot.config.MqttBrokerProperties;
import com.boonya.lab.io.iot.mqtt.MqttClientWrapper;
import io.moquette.interception.AbstractInterceptHandler;
import io.moquette.interception.messages.InterceptPublishMessage;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@ConditionalOnBean(EmbeddedMqttBroker.class)
@RequiredArgsConstructor
public class EmbeddedMqttClientWrapper implements MqttClientWrapper {

    private final EmbeddedMqttBroker embeddedMqttBroker;
    private final MqttBrokerProperties brokerProperties;
    private final Map<String, MessageHandler> handlers = new ConcurrentHashMap<>();
    private MqttClient internalClient;

    @PostConstruct
    public void init() {
        try {
            String clientId = "embedded-wrapper-" + System.currentTimeMillis();
            String brokerUrl = "tcp://localhost:" + brokerProperties.getEmbeddedPort();
            internalClient = new MqttClient(brokerUrl, clientId, new MemoryPersistence());

            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);
            options.setConnectionTimeout(10);

            internalClient.connect(options);
            log.info("Internal MQTT client connected to embedded broker at {}", brokerUrl);
        } catch (MqttException e) {
            log.error("Failed to create internal MQTT client", e);
            throw new RuntimeException("Failed to initialize embedded MQTT client", e);
        }

        // 注册拦截器处理订阅消息（仅使用拦截器机制，不需要双重订阅）
        embeddedMqttBroker.getMqttBroker().addInterceptHandler(new AbstractInterceptHandler() {
            @Override
            public String getID() {
                return "embedded-mqtt-client-wrapper";
            }

            @Override
            public void onPublish(InterceptPublishMessage msg) {
                String topic = msg.getTopicName();
                MessageHandler handler = handlers.get(topic);
                if (handler != null) {
                    handler.onMessage(topic, msg.getPayload().array());
                }
            }

            @Override
            public void onSessionLoopError(Throwable throwable) {
                log.error("Error in session loop", throwable);
            }
        });
        log.info("Embedded MQTT client wrapper initialized");
    }

    @PreDestroy
    public void destroy() {
        try {
            if (internalClient != null && internalClient.isConnected()) {
                internalClient.disconnect();
                internalClient.close();
            }
        } catch (MqttException e) {
            log.error("Error disconnecting internal client", e);
        }
    }

    @Override
    public void publish(String topic, byte[] payload, int qos) throws Exception {
        MqttMessage message = new MqttMessage(payload);
        message.setQos(qos);
        internalClient.publish(topic, message);
    }

    @Override
    public void subscribe(String topic, MessageHandler handler) throws Exception {
        // 只需要将 handler 注册到 map，拦截器会自动接收消息
        handlers.put(topic, handler);
        log.info("Registered handler for topic: {}", topic);
    }

    @Override
    public void disconnect() throws Exception {
        handlers.clear();
        try {
            if (internalClient != null && internalClient.isConnected()) {
                internalClient.disconnect();
            }
        } catch (MqttException e) {
            log.error("Error disconnecting", e);
            throw e;
        }
        log.info("Disconnected all handlers");
    }
}
