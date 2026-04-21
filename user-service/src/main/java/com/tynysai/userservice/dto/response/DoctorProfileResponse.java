package com.tynysai.userservice.dto.response;

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
public class DoctorProfileResponse {
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
    private String specialization;
    private String licenseNumber;
    private String hospitalName;
    private String department;
    private Integer yearsOfExperience;
    private String bio;
    private String education;
    private boolean approved;
    private String workSchedule;

    private LocalDateTime profileCreatedAt;
}
