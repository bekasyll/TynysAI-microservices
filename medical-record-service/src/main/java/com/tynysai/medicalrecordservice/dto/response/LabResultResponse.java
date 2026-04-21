package com.tynysai.medicalrecordservice.dto.response;

import com.tynysai.medicalrecordservice.model.enums.LabTestType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabResultResponse {
    private Long id;
    private UUID patientId;
    private String patientName;
    private UUID addedByDoctorId;
    private String addedByDoctorName;

    private LabTestType testType;
    private String testTypeDisplayName;
    private LocalDate testDate;
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

    private String cultureResult;
    private String pathogenFound;
    private String sensitivityResult;

    private String igraResult;
    private String mantouxResult;
    private Integer mantouxInduratMm;

    private String pcrResult;
    private Double pcrCtValue;

    private String notes;
    private String rawResultText;
    private LocalDateTime createdAt;
}
