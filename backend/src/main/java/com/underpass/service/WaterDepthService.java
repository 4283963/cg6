package com.underpass.service;

import com.underpass.dto.SensorDataDTO;
import com.underpass.dto.UnderpassStatusVO;
import com.underpass.entity.WaterDepthRecord;
import com.underpass.repository.WaterDepthRecordRepository;
import com.underpass.repository.UnderpassInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class WaterDepthService {

    private final WaterDepthRecordRepository depthRepo;
    private final UnderpassInfoRepository underpassRepo;
    private final FloodTriggerEngine triggerEngine;
    private final DepthFilterService depthFilterService;
    private final SimpMessagingTemplate wsTemplate;

    private final Map<String, Double> latestRawDepthMap = new ConcurrentHashMap<>();
    private final Map<String, Double> latestSmoothedDepthMap = new ConcurrentHashMap<>();

    public void processSensorData(SensorDataDTO data) {
        WaterDepthRecord record = new WaterDepthRecord();
        record.setUnderpassId(data.getUnderpassId());
        record.setSensorId(data.getSensorId());
        record.setDepthMm(data.getDepthMm());
        record.setTimestampMs(data.getTimestamp());
        depthRepo.save(record);

        double smoothed = depthFilterService.filter(data.getUnderpassId(), data.getDepthMm());

        latestRawDepthMap.put(data.getUnderpassId(), data.getDepthMm());
        latestSmoothedDepthMap.put(data.getUnderpassId(), smoothed);

        triggerEngine.evaluate(data.getUnderpassId(), data.getDepthMm());

        pushRealtimeUpdate(data.getUnderpassId());
    }

    public Double getLatestDepth(String underpassId) {
        return latestSmoothedDepthMap.get(underpassId);
    }

    public Double getLatestRawDepth(String underpassId) {
        return latestRawDepthMap.get(underpassId);
    }

    public List<WaterDepthRecord> getHistory(String underpassId) {
        return depthRepo.findTop20ByUnderpassIdOrderByReceivedAtDesc(underpassId);
    }

    public UnderpassStatusVO buildStatusVO(String underpassId) {
        UnderpassStatusVO vo = new UnderpassStatusVO();
        vo.setUnderpassId(underpassId);

        underpassRepo.findById(underpassId).ifPresent(info -> {
            vo.setName(info.getName());
            vo.setLongitude(info.getLongitude());
            vo.setLatitude(info.getLatitude());
            vo.setStatus(info.getStatus());
        });

        Double smoothed = latestSmoothedDepthMap.get(underpassId);
        Double raw = latestRawDepthMap.get(underpassId);
        vo.setCurrentDepthMm(smoothed);
        vo.setRawDepthMm(raw);
        vo.setLastUpdateTime(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return vo;
    }

    private void pushRealtimeUpdate(String underpassId) {
        try {
            UnderpassStatusVO vo = buildStatusVO(underpassId);
            wsTemplate.convertAndSend("/topic/underpass/update", vo);
        } catch (Exception e) {
            log.error("WebSocket push failed: {}", e.getMessage());
        }
    }
}
