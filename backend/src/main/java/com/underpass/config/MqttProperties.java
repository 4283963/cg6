package com.underpass.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "mqtt")
public class MqttProperties {
    private String broker;
    private String clientId;
    private String username;
    private String password;
    private Topic topic = new Topic();

    @Data
    public static class Topic {
        private String sensorData;
        private String commandPrefix;
        private String hydraulicStatus;
    }
}
