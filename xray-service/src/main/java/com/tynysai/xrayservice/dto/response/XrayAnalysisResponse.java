package com.tynysai.xrayservice.dto.response;

import com.tynysai.xrayservice.model.enums.AnalysisStatus;
import com.tynysai.xrayservice.model.enums.DiseaseType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XrayAnalysisResponse {
    private Long id;

    private Long patientId;
    private String patientName;

    private Long assignedDoctorId;
    private String assignedDoctorName;

    private String originalFileName;
    private String contentType;
    private Long fileSizeBytes;

    private AnalysisStatus status;

    private DiseaseType aiPrimaryDiagnosis;
    private String aiPrimaryDiagnosisDisplayName;
    private Double aiConfidence;
    private String aiFindings;
    private String aiDetectedAbnormalities;
    private String aiAllPredictionsJson;

    private Long validatedByDoctorId;
    private String validatedByDoctorName;
    private DiseaseType doctorDiagnosis;
    private String doctorDiagnosisDisplayName;
    private String doctorNotes;
    private LocalDateTime validatedAt;

    private String patientNotes;
    private LocalDateTime uploadedAt;
    private LocalDateTime analyzedAt;
}
