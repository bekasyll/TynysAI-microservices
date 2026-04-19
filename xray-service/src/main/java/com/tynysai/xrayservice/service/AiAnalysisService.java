package com.tynysai.xrayservice.service;

import com.tynysai.xrayservice.dto.response.AiAnalysisResult;
import com.tynysai.xrayservice.dto.response.PythonAnalysisResponse;
import com.tynysai.xrayservice.exception.FileStorageException;
import com.tynysai.xrayservice.model.enums.DiseaseType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiAnalysisService {
    @Value("${app.ai.enabled:false}")
    private boolean aiEnabled;

    @Value("${app.ai.confidence-threshold:0.70}")
    private double confidenceThreshold;

    private final RestClient aiClient;

    public AiAnalysisResult analyzeImage(String imagePath) {
        log.info("AI analysis requested for: {}", imagePath);
        if (!aiEnabled) {
            log.info("AI module disabled — returning stub result");
            return buildStubResult();
        }
        try {
            return callAiService(imagePath);
        } catch (Exception e) {
            log.error("AI service call failed: {}. Falling back to stub.", e.getMessage());
            return buildStubResult();
        }
    }

    private AiAnalysisResult callAiService(String imagePath) {
        File imageFile = new File(imagePath);
        if (!imageFile.exists()) {
            throw new FileStorageException("Image file not found: " + imagePath);
        }

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(imageFile));

        PythonAnalysisResponse python = aiClient.post()
                .uri("/analyze")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(PythonAnalysisResponse.class);

        if (python == null) {
            throw new RuntimeException("Empty response from AI service");
        }
        return mapToResult(python);
    }

    private AiAnalysisResult mapToResult(PythonAnalysisResponse python) {
        boolean isPneumonia = "PNEUMONIA".equalsIgnoreCase(python.getDiagnosis());
        DiseaseType primary = isPneumonia ? DiseaseType.BACTERIAL_PNEUMONIA : DiseaseType.NORMAL;

        Map<DiseaseType, Double> predictions = new LinkedHashMap<>();
        if (isPneumonia) {
            predictions.put(DiseaseType.BACTERIAL_PNEUMONIA, python.getConfidence());
            predictions.put(DiseaseType.NORMAL, 1.0 - python.getConfidence());
        } else {
            predictions.put(DiseaseType.NORMAL, python.getConfidence());
            predictions.put(DiseaseType.BACTERIAL_PNEUMONIA, 1.0 - python.getConfidence());
        }

        List<String> abnormalities = isPneumonia
                ? List.of("Pulmonary consolidation", "Increased opacity")
                : List.of();

        boolean requiresReview = python.isRequiresDoctorReview() || python.getConfidence() < confidenceThreshold;

        return AiAnalysisResult.builder()
                .primaryDiagnosis(primary)
                .primaryConfidence(python.getConfidence())
                .allPredictions(predictions)
                .findings(python.getFindings())
                .detectedAbnormalities(abnormalities)
                .requiresDoctorReview(requiresReview)
                .modelVersion(python.getModelVersion())
                .rawScore(python.getRawScore())
                .severity(python.getSeverity())
                .build();
    }

    private AiAnalysisResult buildStubResult() {
        Map<DiseaseType, Double> predictions = new LinkedHashMap<>();
        predictions.put(DiseaseType.NORMAL, 0.87);
        predictions.put(DiseaseType.BACTERIAL_PNEUMONIA, 0.13);

        return AiAnalysisResult.builder()
                .primaryDiagnosis(DiseaseType.NORMAL)
                .primaryConfidence(0.87)
                .allPredictions(predictions)
                .findings("Stub result — AI service not running.")
                .detectedAbnormalities(List.of())
                .requiresDoctorReview(true)
                .modelVersion("stub-1.0")
                .rawScore(0.87)
                .build();
    }
}
