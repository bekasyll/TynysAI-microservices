package com.tynysai.appointmentservice.dto.request;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AppointmentDecisionRequest {
    private String doctorNotes;
    private LocalDateTime appointmentDate;
}
