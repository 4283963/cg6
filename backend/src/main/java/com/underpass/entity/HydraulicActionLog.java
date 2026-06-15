package com.underpass.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "hydraulic_action_log")
public class HydraulicActionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String underpassId;

    @Column(nullable = false, length = 16)
    private String action;

    @Column(nullable = false)
    private Integer heightCm;

    @Column(length = 16)
    private String status;

    private LocalDateTime triggerTime;

    private LocalDateTime completeTime;

    @PrePersist
    public void prePersist() {
        if (triggerTime == null) {
            triggerTime = LocalDateTime.now();
        }
    }
}
