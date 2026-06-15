package com.underpass.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "water_depth_record")
public class WaterDepthRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String underpassId;

    @Column(nullable = false, length = 32)
    private String sensorId;

    @Column(nullable = false)
    private Double depthMm;

    private Long timestampMs;

    @Column(nullable = false)
    private LocalDateTime receivedAt;

    @PrePersist
    public void prePersist() {
        receivedAt = LocalDateTime.now();
        if (timestampMs == null) {
            timestampMs = System.currentTimeMillis();
        }
    }
}
