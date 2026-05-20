package com.boonya.lab.io.iot.mqtt.impl;


import com.boonya.lab.io.iot.mqtt.MqttClientWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Slf4j
@Component
@ConditionalOnBean(MqttClient.class)
@RequiredArgsConstructor
public class EmqxMqttClientWrapper implements MqttClientWrapper {

    private final MqttClient mqttClient;

    @Override
    public void publish(String topic, byte[] payload, int qos) throws Exception {
        MqttMessage message = new MqttMessage(payload);
        message.setQos(qos);
        mqttClient.publish(topic, message);
    }

    @Override
    public void subscribe(String topic, MessageHandler handler) throws Exception {
        mqttClient.subscribe(topic, (IMqttMessageListener) (t, msg) ->
                handler.onMessage(t, msg.getPayload())
        );
        log.info("Subscribed to topic: {}", topic);
    }

    @Override
    public void disconnect() throws Exception {
        if (mqttClient != null && mqttClient.isConnected()) {
            mqttClient.disconnect();
            mqttClient.close();
            log.info("Disconnected and closed EMQX client");
        }
    }
}
