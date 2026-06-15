package com.underpass.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "rainfall_record")
public class RainfallRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String underpassId;

    @Column(length = 32)
    private String sensorId;

    @Column(nullable = false)
    private Boolean raining;

    @Column(nullable = false)
    private Double rainMmPerHour;

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
