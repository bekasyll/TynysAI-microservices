package com.tynysai.xrayservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GradcamResponse {
    private String heatmapB64;
    private String overlayB64;
    private double activePercent;
    private String targetClass;
    private String modelUsed;
}
