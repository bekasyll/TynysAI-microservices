package com.tynysai.xrayservice.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GradcamRequest {
    @NotBlank
    private String targetClass;

    private Double alpha;

    private Double threshold;

    private String colormap;

    public double getAlphaOrDefault() { return alpha != null ? alpha : 0.5; }
    public double getThresholdOrDefault() { return threshold != null ? threshold : 0.3; }
    public String getColormapOrDefault() { return colormap != null ? colormap : "jet"; }
}
