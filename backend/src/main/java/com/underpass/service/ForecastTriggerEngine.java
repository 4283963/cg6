package com.underpass.service;

import com.underpass.config.FloodProperties;
import com.underpass.dto.HydraulicCommandDTO;
import com.underpass.entity.FlowRecord;
import com.underpass.entity.RainfallRecord;
import com.underpass.entity.UnderpassInfo;
import com.underpass.entity.UpstreamCatchment;
import com.underpass.mqtt.MqttPublisher;
import com.underpass.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ForecastTriggerEngine {

    private final FloodProperties floodProps;
    private final MqttPublisher mqttPublisher;
    private final UnderpassInfoRepository underpassRepo;
    private final UpstreamCatchmentRepository catchmentRepo;
    private final FlowRecordRepository flowRepo;
    private final RainfallRecordRepository rainfallRepo;
    private final HydraulicActionLogRepository hydraulicLogRepo;

    private final Set<String> forecastTriggeredUnderpasses = ConcurrentHashMap.newKeySet();
    private final Map<String, LocalDateTime> lastForecastTime = new ConcurrentHashMap<>();

    public void evaluateFlow(String catchmentId) {
        catchmentRepo.findById(catchmentId).ifPresent(catchment -> {
            List<UnderpassInfo> linkedUnderpasses = underpassRepo.findAll().stream()
                    .filter(u -> catchmentId.equals(u.getUpstreamCatchmentId()))
                    .toList();

            for (UnderpassInfo underpass : linkedUnderpasses) {
                evaluateUnderpassForecast(underpass, catchment);
            }
        });
    }

    private void evaluateUnderpassForecast(UnderpassInfo underpass, UpstreamCatchment catchment) {
        FloodProperties.Forecast cfg = floodProps.getForecast();

        if (isInForecastCooldown(underpass.getId())) {
            log.debug("Underpass {} in forecast cooldown, skip", underpass.getId());
            return;
        }

        if ("ALARM".equals(underpass.getStatus()) || "FULL_LIFTED".equals(underpass.getHydraulicState())) {
            return;
        }

        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(cfg.getFlowWindowMinutes());
        List<FlowRecord> flowHistory = flowRepo
                .findByCatchmentIdAndReceivedAtAfterOrderByReceivedAtDesc(catchment.getId(), windowStart);

        if (flowHistory.size() < 2) {
            return;
        }

        double currentFlow = flowHistory.get(0).getFlowRateLps();
        double oldestFlowInWindow = flowHistory.get(flowHistory.size() - 1).getFlowRateLps();

        if (oldestFlowInWindow <= 0) {
            return;
        }

        double surgeRatio = currentFlow / oldestFlowInWindow;

        List<RainfallRecord> recentRain = rainfallRepo
                .findTop1ByUnderpassIdOrderByReceivedAtDesc(underpass.getId());
        boolean isRaining = !recentRain.isEmpty()
                && recentRain.get(0).getRaining()
                && recentRain.get(0).getRainMmPerHour() >= cfg.getMinRainMmPerHour();

        log.info("Forecast check for {}: currentFlow={:.1f} L/s, 10minAgo={:.1f} L/s, ratio={:.2f}, raining={}",
                underpass.getId(), currentFlow, oldestFlowInWindow, surgeRatio, isRaining);

        if (surgeRatio >= cfg.getFlowSurgeRatio() && isRaining) {
            log.warn("!!! FORECAST TRIGGER for {}: flow surged {:.2f}x in {}min + raining -> PRE-LIFT {}cm !!!",
                    underpass.getId(), surgeRatio, cfg.getFlowWindowMinutes(), cfg.getPreLiftHeightCm());
            triggerPreLift(underpass);
        }
    }

    private boolean isInForecastCooldown(String underpassId) {
        LocalDateTime last = lastForecastTime.get(underpassId);
        if (last == null) {
            return false;
        }
        long elapsedMin = Duration.between(last, LocalDateTime.now()).toMinutes();
        return elapsedMin < floodProps.getForecast().getForecastCooldownMinutes();
    }

    private void triggerPreLift(UnderpassInfo underpass) {
        FloodProperties.Forecast cfg = floodProps.getForecast();

        HydraulicCommandDTO cmd = new HydraulicCommandDTO();
        cmd.setUnderpassId(underpass.getId());
        cmd.setAction("prelift");
        cmd.setHeightCm(cfg.getPreLiftHeightCm());

        String topic = "underpass/command/" + underpass.getId() + "/hydraulic";
        mqttPublisher.publish(topic, cmd);

        underpass.setHydraulicState("PRE_LIFTED");
        underpass.setLastForecastTime(LocalDateTime.now());
        underpassRepo.save(underpass);

        forecastTriggeredUnderpasses.add(underpass.getId());
        lastForecastTime.put(underpass.getId(), LocalDateTime.now());

        com.underpass.entity.HydraulicActionLog logEntry = new com.underpass.entity.HydraulicActionLog();
        logEntry.setUnderpassId(underpass.getId());
        logEntry.setAction("prelift");
        logEntry.setHeightCm(cfg.getPreLiftHeightCm());
        logEntry.setStatus("dispatched");
        hydraulicLogRepo.save(logEntry);

        log.info("Forecast PRE-LIFT {}cm dispatched for {}", cfg.getPreLiftHeightCm(), underpass.getId());
    }

    public boolean isForecastTriggered(String underpassId) {
        return forecastTriggeredUnderpasses.contains(underpassId);
    }

    public void clearForecast(String underpassId) {
        forecastTriggeredUnderpasses.remove(underpassId);
    }

    public Double getCurrentFlow(String catchmentId) {
        if (catchmentId == null) return null;
        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(floodProps.getForecast().getFlowWindowMinutes());
        List<FlowRecord> records = flowRepo
                .findByCatchmentIdAndReceivedAtAfterOrderByReceivedAtDesc(catchmentId, windowStart);
        return records.isEmpty() ? null : records.get(0).getFlowRateLps();
    }

    public Double getFlow10MinAgo(String catchmentId) {
        if (catchmentId == null) return null;
        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(floodProps.getForecast().getFlowWindowMinutes());
        List<FlowRecord> records = flowRepo
                .findByCatchmentIdAndReceivedAtAfterOrderByReceivedAtDesc(catchmentId, windowStart);
        return records.size() >= 2 ? records.get(records.size() - 1).getFlowRateLps() : null;
    }

    public Boolean isRaining(String underpassId) {
        List<RainfallRecord> records = rainfallRepo.findTop1ByUnderpassIdOrderByReceivedAtDesc(underpassId);
        return records.isEmpty() ? null : records.get(0).getRaining();
    }
}
