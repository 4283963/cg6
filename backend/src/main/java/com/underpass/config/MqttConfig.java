package com.underpass.config;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
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
