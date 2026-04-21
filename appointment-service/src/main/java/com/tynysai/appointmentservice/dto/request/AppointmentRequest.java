package com.tynysai.appointmentservice.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class AppointmentRequest {
    @NotNull(message = "Doctor ID is required")
    private UUID doctorId;

    private LocalDateTime appointmentDate;
    private String patientComplaints;
    private Long xrayAnalysisId;
}
