package com.underpass.dto;

import lombok.Data;

@Data
public class LedControlDTO {
    private String underpassId;
    private String ledId;
    private String mode;
    private String displayText;
    private String color;
}
