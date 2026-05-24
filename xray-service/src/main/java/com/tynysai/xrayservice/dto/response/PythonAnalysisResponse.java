package com.tynysai.xrayservice.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PythonAnalysisResponse {
    private String diagnosis;
    private double confidence;
    @JsonProperty("raw_score")
    private double rawScore;
    private String severity;
    private String findings;
    @JsonProperty("requires_doctor_review")
    private boolean requiresDoctorReview;
    @JsonProperty("model_version")
    private String modelVersion;
    private Map<String, Double> probabilities;
    private List<String> abnormalities;
    @JsonProperty("original_b64")
    private String originalB64;
}
