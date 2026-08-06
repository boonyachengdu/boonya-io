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
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@ConditionalOnBean(EmbeddedMqttBroker.class)
@RequiredArgsConstructor
public class EmbeddedMqttClientWrapper implements MqttClientWrapper, ApplicationListener<ContextRefreshedEvent> {

    private final EmbeddedMqttBroker embeddedMqttBroker;
    private final MqttBrokerProperties brokerProperties;
    private final List<Subscription> subscriptions = new ArrayList<>();
    private MqttClient internalClient;
    private boolean interceptorRegistered = false;

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
            options.setMaxInflight(1000);

            internalClient.connect(options);
            log.info("Internal MQTT client connected to embedded broker at {}", brokerUrl);
        } catch (MqttException e) {
            log.error("Failed to create internal MQTT client", e);
            throw new RuntimeException("Failed to initialize embedded MQTT client", e);
        }

        log.info("Embedded MQTT client wrapper initialized");
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (!interceptorRegistered && embeddedMqttBroker.getMqttBroker() != null) {
            registerInterceptor();
            interceptorRegistered = true;
        }
    }

    private void registerInterceptor() {
        try {
            embeddedMqttBroker.getMqttBroker().addInterceptHandler(new AbstractInterceptHandler() {
                @Override
                public String getID() {
                    return "embedded-mqtt-client-wrapper";
                }

                @Override
                public void onPublish(InterceptPublishMessage msg) {
                    String topic = msg.getTopicName();
                    byte[] payload = readByteBuf(msg.getPayload());
                    handleMessageWithWildcardMatching(topic, payload);
                }

                @Override
                public void onSessionLoopError(Throwable throwable) {
                    log.error("Error in session loop", throwable);
                }
            });
            log.info("MQTT interceptor registered successfully");
        } catch (Exception e) {
            log.error("Failed to register MQTT interceptor", e);
        }
    }

    private void handleMessageWithWildcardMatching(String topic, byte[] payload) {
        for (Subscription subscription : subscriptions) {
            if (matchTopic(subscription.topicFilter, topic)) {
                try {
                    subscription.handler.onMessage(topic, payload);
                } catch (Exception e) {
                    log.error("Error handling message for topic: {}", topic, e);
                }
            }
        }
    }

    private boolean matchTopic(String topicFilter, String topic) {
        String[] filterLevels = topicFilter.split("/");
        String[] topicLevels = topic.split("/");

        int i = 0;
        for (; i < filterLevels.length; i++) {
            String filterLevel = filterLevels[i];

            if ("#".equals(filterLevel)) {
                return true;
            }

            if (i >= topicLevels.length) {
                return false;
            }

            if ("+".equals(filterLevel)) {
                continue;
            }

            if (!filterLevel.equals(topicLevels[i])) {
                return false;
            }
        }

        return i == filterLevels.length && i == topicLevels.length;
    }

    private byte[] readByteBuf(io.netty.buffer.ByteBuf byteBuf) {
        if (byteBuf == null) {
            return new byte[0];
        }

        byte[] payload = new byte[byteBuf.readableBytes()];
        byteBuf.getBytes(byteBuf.readerIndex(), payload);
        return payload;
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
        Subscription subscription = new Subscription(topic, handler);
        subscriptions.add(subscription);
        log.info("Registered handler for topic filter: {}", topic);
    }

    @Override
    public void disconnect() throws Exception {
        subscriptions.clear();
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

    private static class Subscription {
        final String topicFilter;
        final MessageHandler handler;

        Subscription(String topicFilter, MessageHandler handler) {
            this.topicFilter = topicFilter;
            this.handler = handler;
        }
    }
}
