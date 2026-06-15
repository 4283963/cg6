package com.underpass.dto;

import lombok.Data;

@Data
public class HydraulicCommandDTO {
    private String underpassId;
    private String action;
    private Integer heightCm;
}
