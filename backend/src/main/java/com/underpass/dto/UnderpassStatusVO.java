package com.underpass.dto;

import lombok.Data;

@Data
public class UnderpassStatusVO {
    private String underpassId;
    private String name;
    private Double longitude;
    private Double latitude;
    private Double currentDepthMm;
    private Double rawDepthMm;
    private String status;
    private Boolean hydraulicLifted;
    private Boolean ledAlarmActive;
    private String lastUpdateTime;
}
