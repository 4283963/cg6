package com.underpass.config;

import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MqttConfig {

    @Bean
    public MqttClient mqttClient(MqttProperties props) throws MqttException {
        MqttClient client = new MqttClient(
                props.getBroker(),
                props.getClientId() + "-" + System.currentTimeMillis(),
                new MemoryPersistence()
        );
        return client;
    }
}
