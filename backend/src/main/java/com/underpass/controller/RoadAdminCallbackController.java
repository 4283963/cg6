package com.underpass.controller;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/road-admin")
public class RoadAdminCallbackController {

    @PostMapping("/led/callback")
    public Map<String, String> ledCallback(@RequestBody Map<String, Object> payload) {
        String ledId = (String) payload.getOrDefault("ledId", "unknown");
        String status = (String) payload.getOrDefault("status", "unknown");
        return Map.of("received", "ok", "ledId", ledId, "status", status);
    }
}
