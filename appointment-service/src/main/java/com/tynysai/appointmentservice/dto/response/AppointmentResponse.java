package com.tynysai.appointmentservice.dto.response;

import com.tynysai.appointmentservice.model.enums.AppointmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentResponse {
    private Long id;
    private UUID patientId;
    private String patientName;
    private UUID doctorId;
    private String doctorName;
    private String doctorSpecialization;
    private AppointmentStatus status;
    private LocalDateTime appointmentDate;
    private String patientComplaints;
    private String doctorNotes;
    private Long reportId;
    private Long xrayAnalysisId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
