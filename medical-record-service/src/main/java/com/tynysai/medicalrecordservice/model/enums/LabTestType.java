package com.tynysai.medicalrecordservice.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LabTestType {
    COMPLETE_BLOOD_COUNT("Complete Blood Count (CBC)"),
    BIOCHEMISTRY("Biochemical Analysis"),
    SPUTUM_CULTURE("Sputum Culture"),
    PCR_COVID("PCR Test - COVID-19"),
    PCR_TB("PCR Test - Tuberculosis"),
    IGRA_TB("IGRA Test - Tuberculosis"),
    MANTOUX("Mantoux Tuberculin Skin Test"),
    BLOOD_GAS("Arterial Blood Gas (ABG)"),
    SPIROMETRY("Spirometry"),
    CULTURE_SENSITIVITY("Culture & Sensitivity"),
    INFLAMMATORY_MARKERS("Inflammatory Markers (CRP, ESR)"),
    OTHER("Other");

    private final String displayName;
}
