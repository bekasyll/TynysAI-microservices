package com.tynysai.medicalrecordservice.dto.response;

import com.tynysai.medicalrecordservice.model.enums.DiseaseType;
import com.tynysai.medicalrecordservice.model.enums.Severity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiagnosticReportResponse {
    private Long id;
    private String reportNumber;

    private Long patientId;
    private String patientName;

    private Long doctorId;
    private String doctorName;
    private String doctorSpecialization;

    private Long xrayAnalysisId;
    private Long labResultId;

    private DiseaseType finalDiagnosis;
    private String finalDiagnosisDisplayName;
    private Severity severity;
    private String severityDisplayName;

    private String clinicalFindings;
    private String treatmentRecommendations;
    private String medicationRecommendations;
    private String lifestyleRecommendations;

    private LocalDate followUpDate;
    private String reportText;

    private boolean sentToPatient;
    private LocalDateTime sentAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
