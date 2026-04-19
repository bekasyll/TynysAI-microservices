package com.tynysai.xrayservice.dto.request;

import com.tynysai.xrayservice.model.enums.DiseaseType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DoctorValidationRequest {
    @NotNull(message = "Doctor diagnosis is required")
    private DiseaseType doctorDiagnosis;

    private String doctorNotes;
    private boolean agreesWithAi = true;
}
