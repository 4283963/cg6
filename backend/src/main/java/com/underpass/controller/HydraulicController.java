package com.underpass.controller;

import com.underpass.dto.HydraulicCommandDTO;
import com.underpass.entity.HydraulicActionLog;
import com.underpass.mqtt.MqttPublisher;
import com.underpass.repository.HydraulicActionLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/hydraulic")
@RequiredArgsConstructor
public class HydraulicController {

    private final MqttPublisher mqttPublisher;
    private final HydraulicActionLogRepository logRepo;

    @PostMapping("/command")
    public ResponseEntity<Map<String, String>> sendCommand(@RequestBody HydraulicCommandDTO cmd) {
        String topic = "underpass/command/" + cmd.getUnderpassId() + "/hydraulic";
        mqttPublisher.publish(topic, cmd);

        HydraulicActionLog logEntry = new HydraulicActionLog();
        logEntry.setUnderpassId(cmd.getUnderpassId());
        logEntry.setAction(cmd.getAction());
        logEntry.setHeightCm(cmd.getHeightCm() != null ? cmd.getHeightCm() : 0);
        logEntry.setStatus("dispatched");
        logRepo.save(logEntry);

        return ResponseEntity.ok(Map.of(
                "result", "ok",
                "underpassId", cmd.getUnderpassId(),
                "action", cmd.getAction()
        ));
    }
}
