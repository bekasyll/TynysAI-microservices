package com.tynysai.medicalrecordservice.model;

import com.tynysai.medicalrecordservice.model.enums.DiseaseType;
import com.tynysai.medicalrecordservice.model.enums.Severity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "diagnostic_reports", indexes = {
        @Index(name = "idx_report_patient", columnList = "patient_id"),
        @Index(name = "idx_report_doctor", columnList = "doctor_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiagnosticReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false, nullable = false)
    private Long id;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "doctor_id", nullable = false)
    private Long doctorId;

    @Column(name = "xray_analysis_id")
    private Long xrayAnalysisId;

    @Column(name = "lab_result_id")
    private Long labResultId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DiseaseType finalDiagnosis;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private Severity severity = Severity.NONE;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String clinicalFindings;

    @Column(columnDefinition = "TEXT")
    private String treatmentRecommendations;

    @Column(columnDefinition = "TEXT")
    private String medicationRecommendations;

    @Column(columnDefinition = "TEXT")
    private String lifestyleRecommendations;

    private LocalDate followUpDate;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reportText;

    @Column(length = 500)
    private String attachmentPath;

    @Column(nullable = false)
    @Builder.Default
    private boolean sentToPatient = false;

    private LocalDateTime sentAt;

    private String reportNumber;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
