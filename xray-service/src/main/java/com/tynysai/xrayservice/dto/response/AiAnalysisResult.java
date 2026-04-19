package com.tynysai.xrayservice.dto.response;

import com.tynysai.xrayservice.model.enums.DiseaseType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiAnalysisResult {
    private DiseaseType primaryDiagnosis;
    private double primaryConfidence;
    private Map<DiseaseType, Double> allPredictions;
    private String findings;
    private List<String> detectedAbnormalities;
    private boolean requiresDoctorReview;
    private String modelVersion;
    private double rawScore;
    private String severity;
}
