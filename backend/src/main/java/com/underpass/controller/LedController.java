package com.underpass.controller;

import com.underpass.dto.LedControlDTO;
import com.underpass.service.LedControlService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/led")
@RequiredArgsConstructor
public class LedController {

    private final LedControlService ledControlService;

    @PostMapping("/control")
    public ResponseEntity<Map<String, String>> controlLed(@RequestBody LedControlDTO dto) {
        ledControlService.sendAlarm(dto);
        return ResponseEntity.ok(Map.of(
                "result", "ok",
                "ledId", dto.getLedId() != null ? dto.getLedId() : "",
                "mode", dto.getMode()
        ));
    }
}
