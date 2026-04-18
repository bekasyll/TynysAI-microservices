package com.tynysai.medicalrecordservice.model;

import com.tynysai.medicalrecordservice.model.enums.LabTestType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "lab_results", indexes = {
        @Index(name = "idx_lab_patient", columnList = "patient_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false, nullable = false)
    private Long id;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "added_by_doctor_id")
    private Long addedByDoctorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private LabTestType testType;

    @Column(nullable = false)
    private LocalDate testDate;

    @Column(length = 150)
    private String labName;

    private Double hemoglobin;
    private Double wbc;
    private Double rbc;
    private Double platelets;
    private Double hematocrit;
    private Double neutrophils;
    private Double lymphocytes;
    private Double monocytes;
    private Double eosinophils;

    private Double crp;
    private Double esr;
    private Double proCalcitonin;
    private Double ferritin;
    private Double ldh;
    private Double dDimer;

    private Double glucose;
    private Double creatinine;
    private Double urea;
    private Double albumin;
    private Double totalProtein;
    private Double alt;
    private Double ast;
    private Double bilirubin;

    private Double ph;
    private Double pao2;
    private Double paco2;
    private Double hco3;
    private Double spo2;

    private Double fev1;
    private Double fvc;
    private Double fev1FvcRatio;

    @Column(length = 500)
    private String cultureResult;

    @Column(length = 200)
    private String pathogenFound;

    @Column(length = 500)
    private String sensitivityResult;

    @Column(length = 10)
    private String igraResult;

    @Column(length = 10)
    private String mantouxResult;

    private Integer mantouxInduratMm;

    @Column(length = 20)
    private String pcrResult;

    private Double pcrCtValue;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(columnDefinition = "TEXT")
    private String rawResultText;

    @Column(length = 500)
    private String attachmentPath;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
