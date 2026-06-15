package com.underpass.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MqttPublisher {

    private final MqttClient mqttClient;
    private final ObjectMapper objectMapper;

    public void publish(String topic, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            MqttMessage message = new MqttMessage(json.getBytes());
            message.setQos(1);
            mqttClient.publish(topic, message);
            log.debug("Published to {}: {}", topic, json);
        } catch (MqttException e) {
            log.error("MQTT publish failed to {}: {}", topic, e.getMessage());
        } catch (Exception e) {
            log.error("Serialization failed for topic {}: {}", topic, e.getMessage());
        }
    }
}
