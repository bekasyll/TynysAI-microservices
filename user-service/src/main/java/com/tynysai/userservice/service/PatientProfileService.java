package com.tynysai.userservice.service;

import com.tynysai.userservice.dto.PageResponse;
import com.tynysai.userservice.dto.request.UpdatePatientProfileRequest;
import com.tynysai.userservice.dto.response.PatientProfileResponse;
import com.tynysai.userservice.exception.ResourceNotFoundException;
import com.tynysai.userservice.mapper.UserMapper;
import com.tynysai.userservice.model.PatientProfile;
import com.tynysai.userservice.model.User;
import com.tynysai.userservice.model.enums.Role;
import com.tynysai.userservice.repository.PatientProfileRepository;
import com.tynysai.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PatientProfileService {
    private final PatientProfileRepository patientProfileRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    @Transactional
    public PatientProfileResponse getMyProfile(UUID id) {
        User user = userService.findById(id);
        PatientProfile patientProfile = getOrCreate(user);
        return UserMapper.toPatientResponse(patientProfile, user);
    }

    public PatientProfileResponse getByUserId(UUID patientUserId) {
        User user = userService.findById(patientUserId);
        PatientProfile patientProfile = patientProfileRepository.findByUserId(patientUserId)
                .orElseThrow(() -> new ResourceNotFoundException("PatientProfile", "userId", patientUserId));
        return UserMapper.toPatientResponse(patientProfile, user);
    }

    @Transactional
    public PatientProfileResponse updateMyProfile(UUID userId, UpdatePatientProfileRequest request) {
        User user = userService.findById(userId);
        PatientProfile patientProfile = getOrCreate(user);

        if (request.getDateOfBirth() != null) patientProfile.setDateOfBirth(request.getDateOfBirth());
        if (request.getGender() != null) patientProfile.setGender(request.getGender());
        if (request.getBloodType() != null) patientProfile.setBloodType(request.getBloodType());
        if (request.getHeightCm() != null) patientProfile.setHeightCm(request.getHeightCm());
        if (request.getWeightKg() != null) patientProfile.setWeightKg(request.getWeightKg());
        if (request.getAllergies() != null) patientProfile.setAllergies(request.getAllergies());
        if (request.getChronicDiseases() != null) patientProfile.setChronicDiseases(request.getChronicDiseases());
        if (request.getEmergencyContactName() != null)
            patientProfile.setEmergencyContactName(request.getEmergencyContactName());
        if (request.getEmergencyContactPhone() != null)
            patientProfile.setEmergencyContactPhone(request.getEmergencyContactPhone());
        if (request.getAddress() != null) patientProfile.setAddress(request.getAddress());
        if (request.getInsuranceNumber() != null) patientProfile.setInsuranceNumber(request.getInsuranceNumber());
        if (request.getOccupation() != null) patientProfile.setOccupation(request.getOccupation());
        if (request.getSmoker() != null) patientProfile.setSmoker(request.getSmoker());
        if (request.getAlcoholUser() != null) patientProfile.setAlcoholUser(request.getAlcoholUser());
        if (request.getMedicalHistory() != null) patientProfile.setMedicalHistory(request.getMedicalHistory());

        return UserMapper.toPatientResponse(patientProfileRepository.save(patientProfile), user);
    }

    public PageResponse<PatientProfileResponse> listPatients(Pageable pageable) {
        Page<User> patients = userRepository.findByRole(Role.PATIENT, pageable);
        return PageResponse.from(patients.map(u -> patientProfileRepository.findByUserId(u.getId())
                .map(p -> UserMapper.toPatientResponse(p, u))
                .orElseGet(() -> PatientProfileResponse.builder()
                        .userId(u.getId())
                        .email(u.getEmail())
                        .firstName(u.getFirstName())
                        .lastName(u.getLastName())
                        .middleName(u.getMiddleName())
                        .fullName(u.getFullName())
                        .phoneNumber(u.getPhoneNumber())
                        .build())));
    }

    private PatientProfile getOrCreate(User user) {
        return patientProfileRepository.findByUserId(user.getId())
                .orElseGet(() -> patientProfileRepository.save(
                        PatientProfile.builder().userId(user.getId()).build()));
    }
}
