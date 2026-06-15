package com.underpass.service;

import com.underpass.config.FloodProperties;
import com.underpass.dto.HydraulicCommandDTO;
import com.underpass.dto.LedControlDTO;
import com.underpass.entity.HydraulicActionLog;
import com.underpass.entity.LedControlLog;
import com.underpass.entity.UnderpassInfo;
import com.underpass.mqtt.MqttPublisher;
import com.underpass.repository.HydraulicActionLogRepository;
import com.underpass.repository.LedControlLogRepository;
import com.underpass.repository.UnderpassInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class FloodTriggerEngine {

    private final FloodProperties floodProps;
    private final MqttPublisher mqttPublisher;
    private final LedControlService ledControlService;
    private final UnderpassInfoRepository underpassRepo;
    private final HydraulicActionLogRepository hydraulicLogRepo;
    private final LedControlLogRepository ledLogRepo;

    private final Set<String> alarmedUnderpasses = ConcurrentHashMap.newKeySet();

    public void evaluate(String underpassId, double depthMm) {
        if (depthMm >= floodProps.getThresholdMm()) {
            if (!alarmedUnderpasses.contains(underpassId)) {
                log.warn("!!! UNDERPASS {} water depth {}mm >= threshold {}mm, TRIGGERING ALARM !!!",
                        underpassId, depthMm, floodProps.getThresholdMm());
                triggerAlarm(underpassId, depthMm);
            }
        } else {
            if (alarmedUnderpasses.contains(underpassId)) {
                log.info("Underpass {} water depth {}mm below threshold, clearing alarm",
                        underpassId, depthMm);
                clearAlarm(underpassId);
            }
        }
    }

    private void triggerAlarm(String underpassId, double depthMm) {
        alarmedUnderpasses.add(underpassId);

        updateUnderpassStatus(underpassId, "ALARM");

        sendHydraulicLiftCommand(underpassId);

        sendLedAlarmCommand(underpassId);

        logHydraulicAction(underpassId, "lift", floodProps.getLiftHeightCm(), "dispatched");
    }

    private void clearAlarm(String underpassId) {
        alarmedUnderpasses.remove(underpassId);

        updateUnderpassStatus(underpassId, "NORMAL");

        sendHydraulicLowerCommand(underpassId);

        sendLedNormalCommand(underpassId);

        logHydraulicAction(underpassId, "lower", 0, "dispatched");
    }

    private void updateUnderpassStatus(String underpassId, String status) {
        underpassRepo.findById(underpassId).ifPresent(info -> {
            info.setStatus(status);
            if ("ALARM".equals(status)) {
                info.setLastAlarmTime(LocalDateTime.now());
            }
            underpassRepo.save(info);
        });
    }

    private void sendHydraulicLiftCommand(String underpassId) {
        HydraulicCommandDTO cmd = new HydraulicCommandDTO();
        cmd.setUnderpassId(underpassId);
        cmd.setAction("lift");
        cmd.setHeightCm(floodProps.getLiftHeightCm());
        String topic = "underpass/command/" + underpassId + "/hydraulic";
        mqttPublisher.publish(topic, cmd);
        log.info("Dispatched hydraulic LIFT command to underpass {}", underpassId);
    }

    private void sendHydraulicLowerCommand(String underpassId) {
        HydraulicCommandDTO cmd = new HydraulicCommandDTO();
        cmd.setUnderpassId(underpassId);
        cmd.setAction("lower");
        cmd.setHeightCm(0);
        String topic = "underpass/command/" + underpassId + "/hydraulic";
        mqttPublisher.publish(topic, cmd);
        log.info("Dispatched hydraulic LOWER command to underpass {}", underpassId);
    }

    private void sendLedAlarmCommand(String underpassId) {
        underpassRepo.findById(underpassId).ifPresent(info -> {
            LedControlDTO led = new LedControlDTO();
            led.setUnderpassId(underpassId);
            led.setLedId(info.getLedId());
            led.setMode("ALARM");
            led.setDisplayText(floodProps.getThresholdMm() >= 100 ? "积水危险，禁止通行" : "注意积水");
            led.setColor("RED");
            ledControlService.sendAlarm(led);
        });
    }

    private void sendLedNormalCommand(String underpassId) {
        underpassRepo.findById(underpassId).ifPresent(info -> {
            LedControlDTO led = new LedControlDTO();
            led.setUnderpassId(underpassId);
            led.setLedId(info.getLedId());
            led.setMode("NORMAL");
            led.setDisplayText("正常通行");
            led.setColor("GREEN");
            ledControlService.sendAlarm(led);
        });
    }

    private void logHydraulicAction(String underpassId, String action, int heightCm, String status) {
        HydraulicActionLog logEntry = new HydraulicActionLog();
        logEntry.setUnderpassId(underpassId);
        logEntry.setAction(action);
        logEntry.setHeightCm(heightCm);
        logEntry.setStatus(status);
        hydraulicLogRepo.save(logEntry);
    }

    public boolean isAlarmed(String underpassId) {
        return alarmedUnderpasses.contains(underpassId);
    }
}
