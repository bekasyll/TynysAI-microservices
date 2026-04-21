package com.tynysai.medicalrecordservice.client.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class DoctorDto {
    private UUID userId;
    private String specialization;
}
