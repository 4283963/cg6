package com.underpass.dto;

import lombok.Data;

@Data
public class RainfallDataDTO {
    private String underpassId;
    private String sensorId;
    private Boolean raining;
    private Double rainMmPerHour;
    private Long timestamp;
}
