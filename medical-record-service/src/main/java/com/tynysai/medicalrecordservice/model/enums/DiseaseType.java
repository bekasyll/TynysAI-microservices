package com.tynysai.medicalrecordservice.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DiseaseType {
    NORMAL("Normal - No pathology detected"),
    BACTERIAL_PNEUMONIA("Bacterial Pneumonia"),
    VIRAL_PNEUMONIA("Viral Pneumonia"),
    COVID_19("COVID-19 Pneumonia"),
    TUBERCULOSIS("Tuberculosis (TB)"),
    COPD("Chronic Obstructive Pulmonary Disease"),
    LUNG_CANCER("Lung Cancer"),
    PLEURAL_EFFUSION("Pleural Effusion"),
    PNEUMOTHORAX("Pneumothorax"),
    PULMONARY_FIBROSIS("Pulmonary Fibrosis"),
    ATELECTASIS("Atelectasis"),
    CARDIOMEGALY("Cardiomegaly"),
    EDEMA("Pulmonary Edema"),
    OTHER("Other / Requires Further Investigation");

    private final String displayName;
}
