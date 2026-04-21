package com.tynysai.xrayservice.model;

import com.tynysai.xrayservice.model.enums.AnalysisStatus;
import com.tynysai.xrayservice.model.enums.DiseaseType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "xray_analyses", indexes = {
        @Index(name = "idx_xray_patient", columnList = "patient_id"),
        @Index(name = "idx_xray_assigned_doctor", columnList = "assigned_doctor_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class XrayAnalysis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false, nullable = false)
    private Long id;

    @Column(name = "patient_id", columnDefinition = "uuid")
    private UUID patientId;

    @Column(nullable = false, length = 255)
    private String originalFileName;

    @Column(nullable = false, length = 500)
    private String storedFilePath;

    @Column(nullable = false, length = 50)
    private String contentType;

    private Long fileSizeBytes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AnalysisStatus status = AnalysisStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private DiseaseType aiPrimaryDiagnosis;

    private Double aiConfidence;

    @Column(columnDefinition = "TEXT")
    private String aiAllPredictionsJson;

    @Column(columnDefinition = "TEXT")
    private String aiFindings;

    @Column(columnDefinition = "TEXT")
    private String aiDetectedAbnormalities;

    @Column(name = "assigned_doctor_id", columnDefinition = "uuid")
    private UUID assignedDoctorId;

    @Column(name = "validated_by_doctor_id", columnDefinition = "uuid")
    private UUID validatedByDoctorId;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private DiseaseType doctorDiagnosis;

    @Column(columnDefinition = "TEXT")
    private String doctorNotes;

    private LocalDateTime validatedAt;

    @Column(length = 500)
    private String patientNotes;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime uploadedAt;

    private LocalDateTime analyzedAt;
}
