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

import static java.util.Map.entry;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiAnalysisService {
    @Value("${app.ai.enabled:false}")
    private boolean aiEnabled;

    @Value("${app.ai.confidence-threshold:0.70}")
    private double confidenceThreshold;

    private final RestClient aiClient;

    public AiAnalysisResult analyzeImage(String imagePath, String lang) {
        log.info("AI analysis requested for: {} lang={}", imagePath, lang);
        if (!aiEnabled) {
            log.info("AI module disabled - returning stub result");
            return buildStubResult();
        }
        try {
            return callAiService(imagePath, lang);
        } catch (Exception e) {
            log.error("AI service call failed: {}. Falling back to stub.", e.getMessage());
            return buildStubResult();
        }
    }

    private AiAnalysisResult callAiService(String imagePath, String lang) {
        File imageFile = new File(imagePath);
        if (!imageFile.exists()) {
            throw new FileStorageException("Image file not found: " + imagePath);
        }

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(imageFile));
        body.add("lang", lang != null ? lang : "ru");

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

    private static final Map<String, DiseaseType> LABEL_TO_DISEASE = Map.ofEntries(
            entry("COVID19", DiseaseType.COVID_19),
            entry("NORMAL", DiseaseType.NORMAL),
            entry("PNEUMONIA", DiseaseType.BACTERIAL_PNEUMONIA),
            entry("TUBERCULOSIS", DiseaseType.TUBERCULOSIS),
            entry("INVALID", DiseaseType.OTHER)
    );

    private AiAnalysisResult mapToResult(PythonAnalysisResponse python) {
        DiseaseType primary = LABEL_TO_DISEASE.getOrDefault(
                python.getDiagnosis().toUpperCase(), DiseaseType.OTHER);

        Map<DiseaseType, Double> predictions = new LinkedHashMap<>();
        if (python.getProbabilities() != null) {
            python.getProbabilities().forEach((label, prob) -> {
                DiseaseType dt = LABEL_TO_DISEASE.getOrDefault(label.toUpperCase(), DiseaseType.OTHER);
                predictions.merge(dt, prob, Double::max);
            });
        } else {
            predictions.put(primary, python.getConfidence());
        }

        List<String> abnormalities = python.getAbnormalities() != null
                ? python.getAbnormalities()
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
                .originalImageB64(python.getOriginalB64())
                .build();
    }

    public GradcamResult getGradcam(String imageB64, String targetClass, double alpha, double threshold, String colormap) {
        if (!aiEnabled) {
            return null;
        }
        Map<String, Object> body = Map.of(
                "image_b64", imageB64,
                "target_class", targetClass,
                "alpha", alpha,
                "threshold", threshold,
                "colormap", colormap
        );
        try {
            return aiClient.post()
                    .uri("/gradcam")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(GradcamResult.class);
        } catch (Exception e) {
            log.error("Grad-CAM call failed: {}", e.getMessage());
            return null;
        }
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GradcamResult {
        @JsonProperty("heatmap_b64")
        private String heatmapB64;
        @JsonProperty("overlay_b64")
        private String overlayB64;
        @JsonProperty("active_percent")
        private double activePercent;
        @JsonProperty("target_class")
        private String targetClass;
        @JsonProperty("model_used")
        private String modelUsed;
    }

    private AiAnalysisResult buildStubResult() {
        Map<DiseaseType, Double> predictions = new LinkedHashMap<>();
        predictions.put(DiseaseType.NORMAL, 0.87);
        predictions.put(DiseaseType.BACTERIAL_PNEUMONIA, 0.13);

        return AiAnalysisResult.builder()
                .primaryDiagnosis(DiseaseType.NORMAL)
                .primaryConfidence(0.87)
                .allPredictions(predictions)
                .findings("Stub result - AI service not running.")
                .detectedAbnormalities(List.of())
                .requiresDoctorReview(true)
                .modelVersion("stub-1.0")
                .rawScore(0.87)
                .build();
    }
}
