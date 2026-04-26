package com.tynysai.userservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterDoctorRequest {
    @NotBlank
    @Size(max = 255)
    private String firstName;

    @NotBlank
    @Size(max = 255)
    private String lastName;

    @Size(max = 255)
    private String middleName;

    @NotBlank
    @Email
    @Size(max = 255)
    private String email;

    @NotBlank
    @Size(min = 8, max = 64)
    private String password;

    @Pattern(regexp = "^\\+?[1-9]\\d{6,14}$", message = "Invalid phone number format")
    private String phoneNumber;

    @NotBlank
    @Size(max = 255)
    private String specialization;

    @NotBlank
    @Size(max = 255)
    private String licenseNumber;

    @Size(max = 255)
    private String hospitalName;

    @Size(max = 255)
    private String department;

    private Integer yearsOfExperience;

    @Size(max = 500)
    private String bio;

    @Size(max = 500)
    private String education;

    @Size(max = 255)
    private String workSchedule;
}