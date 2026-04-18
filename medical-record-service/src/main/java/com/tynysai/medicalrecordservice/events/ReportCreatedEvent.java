package com.tynysai.medicalrecordservice.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportCreatedEvent {
    private Long reportId;
    private Long patientId;
    private Long doctorId;
    private Long appointmentId;
    private Long xrayAnalysisId;
}
