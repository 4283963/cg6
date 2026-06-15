package com.underpass.dto;

import lombok.Data;

@Data
public class FlowDataDTO {
    private String catchmentId;
    private String flowMeterId;
    private Double flowRateLps;
    private Long timestamp;
}
