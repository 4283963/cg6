package com.underpass.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.underpass.config.MqttProperties;
import com.underpass.dto.FlowDataDTO;
import com.underpass.dto.RainfallDataDTO;
import com.underpass.dto.SensorDataDTO;
import com.underpass.service.UpstreamDataService;
import com.underpass.service.WaterDepthService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MqttSubscriber {

    private final MqttClient mqttClient;
    private final MqttProperties mqttProps;
    private final WaterDepthService waterDepthService;
    private final UpstreamDataService upstreamDataService;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        try {
            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    log.warn("MQTT connection lost: {}", cause.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    handleIncomingMessage(topic, message);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                }
            });

            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);

            String username = mqttProps.getUsername();
            if (username != null && !username.isBlank()) {
                options.setUserName(username);
                options.setPassword(mqttProps.getPassword().toCharArray());
            }

            mqttClient.connect(options);
            subscribeAllTopics();
            log.info("MQTT subscriber initialized");

        } catch (MqttException e) {
            log.error("Failed to initialize MQTT subscriber: {}", e.getMessage());
        }
    }

    private void subscribeAllTopics() {
        try {
            mqttClient.subscribe(mqttProps.getTopic().getSensorData(), 1);
            log.info("Subscribed to sensor topic: {}", mqttProps.getTopic().getSensorData());
            if (mqttProps.getTopic().getFlowData() != null) {
                mqttClient.subscribe(mqttProps.getTopic().getFlowData(), 1);
                log.info("Subscribed to flow topic: {}", mqttProps.getTopic().getFlowData());
            }
            if (mqttProps.getTopic().getRainfallData() != null) {
                mqttClient.subscribe(mqttProps.getTopic().getRainfallData(), 1);
                log.info("Subscribed to rainfall topic: {}", mqttProps.getTopic().getRainfallData());
            }
        } catch (MqttException e) {
            log.error("Failed to subscribe topics: {}", e.getMessage());
        }
    }

    private void handleIncomingMessage(String topic, MqttMessage message) {
        try {
            String payload = new String(message.getPayload());
            log.debug("MQTT message on {}: {}", topic, payload);

            if (topic.equals(mqttProps.getTopic().getSensorData())) {
                SensorDataDTO sensorData = objectMapper.readValue(payload, SensorDataDTO.class);
                waterDepthService.processSensorData(sensorData);
            } else if (topic.equals(mqttProps.getTopic().getFlowData())) {
                FlowDataDTO flowData = objectMapper.readValue(payload, FlowDataDTO.class);
                upstreamDataService.processFlowData(flowData);
            } else if (topic.equals(mqttProps.getTopic().getRainfallData())) {
                RainfallDataDTO rainfallData = objectMapper.readValue(payload, RainfallDataDTO.class);
                upstreamDataService.processRainfallData(rainfallData);
            }
        } catch (Exception e) {
            log.error("Error processing MQTT message on {}: {}", topic, e.getMessage());
        }
    }
}
