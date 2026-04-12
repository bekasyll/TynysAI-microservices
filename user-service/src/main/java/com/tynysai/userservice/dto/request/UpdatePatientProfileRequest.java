package com.tynysai.userservice.dto.request;

import com.tynysai.userservice.model.enums.BloodType;
import com.tynysai.userservice.model.enums.Gender;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdatePatientProfileRequest {
    private LocalDate dateOfBirth;
    private Gender gender;
    private BloodType bloodType;
    private Double heightCm;
    private Double weightKg;
    private String allergies;
    private String chronicDiseases;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String address;
    private String insuranceNumber;
    private String occupation;
    private Boolean smoker;
    private Boolean alcoholUser;
    private String medicalHistory;
}
