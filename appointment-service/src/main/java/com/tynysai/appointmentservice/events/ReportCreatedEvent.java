package com.tynysai.appointmentservice.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReportCreatedEvent {
    private Long reportId;
    private UUID patientId;
    private UUID doctorId;
    private Long appointmentId;
    private Long xrayAnalysisId;
}