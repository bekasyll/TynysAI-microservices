package com.tynysai.appointmentservice.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AppointmentRequest {
    @NotNull(message = "Doctor ID is required")
    private Long doctorId;

    private LocalDateTime appointmentDate;
    private String patientComplaints;
    private Long xrayAnalysisId;
}
