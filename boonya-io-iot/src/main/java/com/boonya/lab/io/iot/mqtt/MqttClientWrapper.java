package com.boonya.lab.io.iot.mqtt;

/**
 * MQTT客户端统一接口
 */
public interface MqttClientWrapper {

    /**
     * 发布消息
     */
    void publish(String topic, byte[] payload, int qos) throws Exception;

    /**
     * 订阅主题
     */
    void subscribe(String topic, MessageHandler handler) throws Exception;

    /**
     * 消息处理器
     */
    @FunctionalInterface
    interface MessageHandler {
        void onMessage(String topic, byte[] payload);
    }

    /**
     * 断开连接
     */
    void disconnect() throws Exception;
}
