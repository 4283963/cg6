package com.underpass.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "led_control_log")
public class LedControlLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String underpassId;

    @Column(nullable = false, length = 32)
    private String ledId;

    @Column(nullable = false, length = 16)
    private String mode;

    @Column(length = 128)
    private String displayText;

    @Column(length = 16)
    private String color;

    @Column(length = 16)
    private String result;

    private LocalDateTime operateTime;

    @PrePersist
    public void prePersist() {
        if (operateTime == null) {
            operateTime = LocalDateTime.now();
        }
    }
}
