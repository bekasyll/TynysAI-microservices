package com.tynysai.userservice.dto.request;

import com.tynysai.userservice.model.enums.Gender;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateDoctorProfileRequest {
    private LocalDate dateOfBirth;
    private Gender gender;
    private String specialization;
    private String licenseNumber;
    private String hospitalName;
    private String department;
    private Integer yearsOfExperience;

    @Size(max = 500)
    private String bio;

    @Size(max = 500)
    private String education;
}
