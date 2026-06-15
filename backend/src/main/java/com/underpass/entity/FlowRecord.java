package com.underpass.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "flow_record")
public class FlowRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String catchmentId;

    @Column(nullable = false, length = 64)
    private String flowMeterId;

    @Column(nullable = false)
    private Double flowRateLps;

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
