package com.underpass.service;

import com.underpass.config.LedProperties;
import com.underpass.dto.LedControlDTO;
import com.underpass.entity.LedControlLog;
import com.underpass.repository.LedControlLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LedControlService {

    private final LedProperties ledProps;
    private final LedControlLogRepository ledLogRepo;
    private final RestTemplate restTemplate = new RestTemplate();

    public void sendAlarm(LedControlDTO dto) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-API-Key", ledProps.getApiKey());

            Map<String, Object> body = Map.of(
                    "ledId", dto.getLedId() != null ? dto.getLedId() : "",
                    "mode", dto.getMode(),
                    "displayText", dto.getDisplayText(),
                    "color", dto.getColor()
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(
                    ledProps.getApiUrl(), request, String.class);

            logLedOperation(dto, response.getStatusCode().is2xxSuccessful() ? "SUCCESS" : "FAILED");

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("LED control SUCCESS: ledId={}, mode={}, text={}",
                        dto.getLedId(), dto.getMode(), dto.getDisplayText());
            } else {
                log.error("LED control FAILED: ledId={}, response={}", dto.getLedId(), response.getBody());
            }
        } catch (Exception e) {
            log.error("LED control API call failed: {}", e.getMessage());
            logLedOperation(dto, "API_ERROR");
        }
    }

    private void logLedOperation(LedControlDTO dto, String result) {
        LedControlLog logEntry = new LedControlLog();
        logEntry.setUnderpassId(dto.getUnderpassId());
        logEntry.setLedId(dto.getLedId());
        logEntry.setMode(dto.getMode());
        logEntry.setDisplayText(dto.getDisplayText());
        logEntry.setColor(dto.getColor());
        logEntry.setResult(result);
        ledLogRepo.save(logEntry);
    }
}
