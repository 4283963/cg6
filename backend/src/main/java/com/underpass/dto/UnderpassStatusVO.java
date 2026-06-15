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
    private String hydraulicState;
    private Boolean hydraulicLifted;
    private Boolean ledAlarmActive;
    private String upstreamCatchmentId;
    private String upstreamCatchmentName;
    private Boolean forecastActive;
    private Double lastFlowRateLps;
    private Double flowRate10MinAgoLps;
    private Boolean currentlyRaining;
    private String lastUpdateTime;
}
