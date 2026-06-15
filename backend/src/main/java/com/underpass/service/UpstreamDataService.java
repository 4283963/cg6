package com.underpass.service;

import com.underpass.dto.FlowDataDTO;
import com.underpass.dto.RainfallDataDTO;
import com.underpass.entity.FlowRecord;
import com.underpass.entity.RainfallRecord;
import com.underpass.entity.UpstreamCatchment;
import com.underpass.repository.FlowRecordRepository;
import com.underpass.repository.RainfallRecordRepository;
import com.underpass.repository.UpstreamCatchmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpstreamDataService {

    private final FlowRecordRepository flowRepo;
    private final RainfallRecordRepository rainfallRepo;
    private final UpstreamCatchmentRepository catchmentRepo;
    private final ForecastTriggerEngine forecastEngine;
    private final SimpMessagingTemplate wsTemplate;

    public void processFlowData(FlowDataDTO data) {
        FlowRecord record = new FlowRecord();
        String catchmentId = data.getCatchmentId();

        if (catchmentId == null || catchmentId.isBlank()) {
            catchmentId = catchmentRepo.findByFlowMeterId(data.getFlowMeterId())
                    .map(UpstreamCatchment::getId)
                    .orElse(null);
        }
        if (catchmentId == null) {
            log.warn("Flow data received but catchment not found for flowMeterId={}", data.getFlowMeterId());
            return;
        }

        record.setCatchmentId(catchmentId);
        record.setFlowMeterId(data.getFlowMeterId());
        record.setFlowRateLps(data.getFlowRateLps());
        record.setTimestampMs(data.getTimestamp());
        flowRepo.save(record);

        log.info("Flow data recorded: catchment={}, flowMeter={}, rate={:.1f} L/s",
                catchmentId, data.getFlowMeterId(), data.getFlowRateLps());

        forecastEngine.evaluateFlow(catchmentId);
    }

    public void processRainfallData(RainfallDataDTO data) {
        RainfallRecord record = new RainfallRecord();
        record.setUnderpassId(data.getUnderpassId());
        record.setSensorId(data.getSensorId());
        record.setRaining(data.getRaining());
        record.setRainMmPerHour(data.getRainMmPerHour() != null ? data.getRainMmPerHour() : 0.0);
        record.setTimestampMs(data.getTimestamp());
        rainfallRepo.save(record);

        log.info("Rainfall data recorded: underpass={}, raining={}, rate={:.1f} mm/h",
                data.getUnderpassId(), data.getRaining(), record.getRainMmPerHour());
    }
}
