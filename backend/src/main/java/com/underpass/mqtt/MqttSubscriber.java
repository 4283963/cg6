package com.underpass.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.underpass.config.MqttProperties;
import com.underpass.dto.SensorDataDTO;
import com.underpass.service.WaterDepthService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MqttSubscriber {

    private final MqttClient mqttClient;
    private final MqttProperties mqttProps;
    private final WaterDepthService waterDepthService;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        try {
            mqttClient.setCallback(new org.eclipse.paho.mqttv5.client.MqttCallback() {
                @Override
                public void disconnected(MqttDisconnectResponse disconnectResponse) {
                    log.warn("MQTT disconnected: {}", disconnectResponse.getReasonString());
                }

                @Override
                public void mqttErrorOccurred(MqttException exception) {
                    log.error("MQTT error: {}", exception.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    handleIncomingMessage(topic, message);
                }

                @Override
                public void deliveryComplete(IMqttToken token) {
                }

                @Override
                public void connectComplete(boolean reconnect, String serverURI) {
                    log.info("MQTT connected to {}, reconnect={}", serverURI, reconnect);
                    subscribeSensorTopic();
                }

                @Override
                public void authPacketArrived(int reasonCode, MqttProperties properties) {
                }
            });

            org.eclipse.paho.mqttv5.common.MqttConnectOptions options =
                    new org.eclipse.paho.mqttv5.common.MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanStart(true);

            String username = mqttProps.getUsername();
            if (username != null && !username.isBlank()) {
                options.setUserName(username);
                options.setPassword(mqttProps.getPassword().getBytes());
            }

            mqttClient.connect(options);
            subscribeSensorTopic();
            log.info("MQTT subscriber initialized, listening to: {}", mqttProps.getTopic().getSensorData());

        } catch (MqttException e) {
            log.error("Failed to initialize MQTT subscriber: {}", e.getMessage());
        }
    }

    private void subscribeSensorTopic() {
        try {
            mqttClient.subscribe(mqttProps.getTopic().getSensorData(), 1);
        } catch (MqttException e) {
            log.error("Failed to subscribe to sensor topic: {}", e.getMessage());
        }
    }

    private void handleIncomingMessage(String topic, MqttMessage message) {
        try {
            String payload = new String(message.getPayload());
            log.debug("MQTT message on {}: {}", topic, payload);

            if (topic.equals(mqttProps.getTopic().getSensorData())) {
                SensorDataDTO sensorData = objectMapper.readValue(payload, SensorDataDTO.class);
                waterDepthService.processSensorData(sensorData);
            }
        } catch (Exception e) {
            log.error("Error processing MQTT message: {}", e.getMessage());
        }
    }
}
