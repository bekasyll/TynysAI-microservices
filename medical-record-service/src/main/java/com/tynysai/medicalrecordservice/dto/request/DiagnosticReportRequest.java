package com.tynysai.medicalrecordservice.dto.request;

import com.tynysai.medicalrecordservice.model.enums.DiseaseType;
import com.tynysai.medicalrecordservice.model.enums.Severity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class DiagnosticReportRequest {
    @NotNull(message = "Patient ID is required")
    private Long patientId;

    private Long appointmentId;
    private Long xrayAnalysisId;
    private Long labResultId;

    @NotNull(message = "Final diagnosis is required")
    private DiseaseType finalDiagnosis;

    @NotNull(message = "Severity is required")
    private Severity severity;

    @NotBlank(message = "Clinical findings are required")
    private String clinicalFindings;

    private String treatmentRecommendations;
    private String medicationRecommendations;
    private String lifestyleRecommendations;

    private LocalDate followUpDate;

    @NotBlank(message = "Report text is required")
    private String reportText;

    private boolean sendToPatient = true;
}
