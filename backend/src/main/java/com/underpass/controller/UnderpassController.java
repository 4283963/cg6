package com.underpass.controller;

import com.underpass.dto.UnderpassStatusVO;
import com.underpass.entity.WaterDepthRecord;
import com.underpass.service.WaterDepthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/underpass")
@RequiredArgsConstructor
public class UnderpassController {

    private final WaterDepthService waterDepthService;
    private final com.underpass.repository.UnderpassInfoRepository underpassRepo;

    @GetMapping("/status")
    public ResponseEntity<List<UnderpassStatusVO>> getAllStatus() {
        List<UnderpassStatusVO> result = new ArrayList<>();
        underpassRepo.findAll().forEach(info -> {
            result.add(waterDepthService.buildStatusVO(info.getId()));
        });
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<UnderpassStatusVO> getStatus(@PathVariable String id) {
        return ResponseEntity.ok(waterDepthService.buildStatusVO(id));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<WaterDepthRecord>> getHistory(@PathVariable String id) {
        return ResponseEntity.ok(waterDepthService.getHistory(id));
    }
}
