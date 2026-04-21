package com.tynysai.userservice.dto.response;

import com.tynysai.userservice.model.enums.BloodType;
import com.tynysai.userservice.model.enums.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientProfileResponse {
    private Long id;
    private UUID userId;
    private String email;
    private String firstName;
    private String lastName;
    private String middleName;
    private String fullName;
    private String phoneNumber;

    private LocalDate dateOfBirth;
    private Integer age;
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

    private LocalDateTime profileCreatedAt;
}
