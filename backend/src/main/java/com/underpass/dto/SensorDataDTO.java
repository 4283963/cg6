package com.underpass.dto;

import lombok.Data;

@Data
public class SensorDataDTO {
    private String underpassId;
    private String sensorId;
    private Double depthMm;
    private Long timestamp;
}
