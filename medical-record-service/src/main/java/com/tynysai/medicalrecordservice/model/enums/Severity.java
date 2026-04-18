package com.tynysai.medicalrecordservice.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Severity {
    NONE("No severity - Normal"),
    MILD("Mild"),
    MODERATE("Moderate"),
    SEVERE("Severe"),
    CRITICAL("Critical");

    private final String displayName;
}
