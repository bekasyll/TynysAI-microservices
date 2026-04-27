package com.tynysai.userservice.mapper;

import com.tynysai.userservice.dto.response.DoctorProfileResponse;
import com.tynysai.userservice.dto.response.PatientProfileResponse;
import com.tynysai.userservice.dto.response.UserResponse;
import com.tynysai.userservice.model.DoctorProfile;
import com.tynysai.userservice.model.PatientProfile;
import com.tynysai.userservice.model.User;

public final class UserMapper {
    private UserMapper() {
    }

    public static UserResponse toUserResponse(User u) {
        return UserResponse.builder()
                .id(u.getId())
                .email(u.getEmail())
                .firstName(u.getFirstName())
                .lastName(u.getLastName())
                .middleName(u.getMiddleName())
                .fullName(u.getFullName())
                .phoneNumber(u.getPhoneNumber())
                .role(u.getRole())
                .enabled(u.isEnabled())
                .emailVerified(u.isEmailVerified())
                .avatarPath(u.getAvatarPath())
                .createdAt(u.getCreatedAt())
                .build();
    }

    public static PatientProfileResponse toPatientResponse(PatientProfile p, User u) {
        return PatientProfileResponse.builder()
                .id(p.getId())
                .userId(u.getId())
                .email(u.getEmail())
                .firstName(u.getFirstName())
                .lastName(u.getLastName())
                .middleName(u.getMiddleName())
                .fullName(u.getFullName())
                .phoneNumber(u.getPhoneNumber())
                .dateOfBirth(p.getDateOfBirth())
                .age(p.getAge())
                .gender(p.getGender())
                .bloodType(p.getBloodType())
                .heightCm(p.getHeightCm())
                .weightKg(p.getWeightKg())
                .allergies(p.getAllergies())
                .chronicDiseases(p.getChronicDiseases())
                .emergencyContactName(p.getEmergencyContactName())
                .emergencyContactPhone(p.getEmergencyContactPhone())
                .address(p.getAddress())
                .insuranceNumber(p.getInsuranceNumber())
                .occupation(p.getOccupation())
                .smoker(p.getSmoker())
                .alcoholUser(p.getAlcoholUser())
                .medicalHistory(p.getMedicalHistory())
                .profileCreatedAt(p.getCreatedAt())
                .build();
    }

    public static DoctorProfileResponse toDoctorResponse(DoctorProfile d, User u) {
        return DoctorProfileResponse.builder()
                .id(d.getId())
                .userId(u.getId())
                .email(u.getEmail())
                .firstName(u.getFirstName())
                .lastName(u.getLastName())
                .middleName(u.getMiddleName())
                .fullName(u.getFullName())
                .phoneNumber(u.getPhoneNumber())
                .dateOfBirth(d.getDateOfBirth())
                .age(d.getAge())
                .gender(d.getGender())
                .specialization(d.getSpecialization())
                .licenseNumber(d.getLicenseNumber())
                .hospitalName(d.getHospitalName())
                .department(d.getDepartment())
                .yearsOfExperience(d.getYearsOfExperience())
                .bio(d.getBio())
                .education(d.getEducation())
                .approved(d.isApproved())
                .profileCreatedAt(d.getCreatedAt())
                .build();
    }
}
