package com.boonya.lab.io.iot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
/**
 * @ClassName: MqttBrokerProperties
 * @Description: TODO(mqtt 切换方案属性)
 * @author: pengjunlin
 * @date:  2026年5月20日
 * @since V1.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "mqtt.broker")
public class MqttBrokerProperties {

    /**
     * broker类型: embedded 或 emqx
     */
    private String type = "embedded";

    /**
     * 外部EMQX的地址（type=emqx时生效）
     */
    private String host = "tcp://localhost:1883";

    /**
     * 嵌入式Broker端口（type=embedded时生效）
     */
    private int embeddedPort = 1883;

    /**
     * 是否允许匿名访问（嵌入式）
     */
    private boolean allowAnonymous = true;
}
