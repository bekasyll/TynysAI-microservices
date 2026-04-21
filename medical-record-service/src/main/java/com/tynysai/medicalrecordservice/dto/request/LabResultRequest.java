package com.tynysai.medicalrecordservice.dto.request;

import com.tynysai.medicalrecordservice.model.enums.LabTestType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class LabResultRequest {
    @NotNull(message = "Patient ID is required")
    private UUID patientId;

    @NotNull(message = "Test type is required")
    private LabTestType testType;

    @NotNull(message = "Test date is required")
    @PastOrPresent(message = "Test date cannot be in the future")
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
}
