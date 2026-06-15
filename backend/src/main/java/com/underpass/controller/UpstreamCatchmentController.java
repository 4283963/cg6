package com.underpass.controller;

import com.underpass.entity.UpstreamCatchment;
import com.underpass.repository.UpstreamCatchmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/upstream")
@RequiredArgsConstructor
public class UpstreamCatchmentController {

    private final UpstreamCatchmentRepository catchmentRepo;

    @GetMapping("/list")
    public ResponseEntity<List<UpstreamCatchment>> list() {
        return ResponseEntity.ok(catchmentRepo.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UpstreamCatchment> get(@PathVariable String id) {
        Optional<UpstreamCatchment> opt = catchmentRepo.findById(id);
        return opt.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<UpstreamCatchment> create(@RequestBody UpstreamCatchment entity) {
        return ResponseEntity.ok(catchmentRepo.save(entity));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UpstreamCatchment> update(
            @PathVariable String id,
            @RequestBody UpstreamCatchment entity) {
        if (!catchmentRepo.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        entity.setId(id);
        return ResponseEntity.ok(catchmentRepo.save(entity));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable String id) {
        catchmentRepo.deleteById(id);
        return ResponseEntity.ok(Map.of("result", "ok"));
    }
}
