package com.tynysai.userservice.service;

import com.tynysai.userservice.dto.request.RegisterDoctorRequest;
import com.tynysai.userservice.dto.request.RegisterPatientRequest;
import com.tynysai.userservice.dto.response.RegisterResponse;
import com.tynysai.userservice.exception.BadRequestException;
import com.tynysai.userservice.kafka.NotificationEventPublisher;
import com.tynysai.userservice.model.DoctorProfile;
import com.tynysai.userservice.model.PatientProfile;
import com.tynysai.userservice.model.User;
import com.tynysai.userservice.model.enums.Role;
import com.tynysai.userservice.repository.DoctorProfileRepository;
import com.tynysai.userservice.repository.PatientProfileRepository;
import com.tynysai.userservice.repository.UserRepository;
import com.tynysai.userservice.security.KeycloakAdminClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final KeycloakAdminClient keycloak;
    private final UserRepository userRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final NotificationEventPublisher notificationPublisher;

    @Transactional
    public RegisterResponse registerPatient(RegisterPatientRequest req) {
        ensureEmailFree(req.getEmail());

        UUID userId = keycloak.createUser(
                req.getEmail(),
                req.getFirstName(),
                req.getLastName(),
                req.getPassword());

        try {
            User user = userRepository.save(User.builder()
                    .id(userId)
                    .email(req.getEmail())
                    .firstName(req.getFirstName())
                    .lastName(req.getLastName())
                    .middleName(StringUtils.defaultString(req.getMiddleName()))
                    .phoneNumber(StringUtils.defaultString(req.getPhoneNumber()))
                    .role(Role.PATIENT)
                    .enabled(true)
                    .emailVerified(true)
                    .build());

            patientProfileRepository.save(PatientProfile.builder()
                    .userId(user.getId())
                    .build());

            return RegisterResponse.builder()
                    .userId(user.getId())
                    .email(user.getEmail())
                    .role(Role.PATIENT)
                    .approved(true)
                    .build();
        } catch (RuntimeException e) {
            log.error("Local DB write failed after Keycloak created {}, rolling back", userId, e);
            keycloak.deleteUserQuietly(userId);
            throw e;
        }
    }

    @Transactional
    public RegisterResponse registerDoctor(RegisterDoctorRequest req) {
        ensureEmailFree(req.getEmail());
        if (StringUtils.isNotBlank(req.getLicenseNumber())
                && doctorProfileRepository.existsByLicenseNumber(req.getLicenseNumber())) {
            throw new BadRequestException("License number is already registered");
        }

        UUID userId = keycloak.createUser(
                req.getEmail(),
                req.getFirstName(),
                req.getLastName(),
                req.getPassword());

        try {
            keycloak.assignRealmRole(userId, "DOCTOR");

            User user = userRepository.save(User.builder()
                    .id(userId)
                    .email(req.getEmail())
                    .firstName(req.getFirstName())
                    .lastName(req.getLastName())
                    .middleName(StringUtils.defaultString(req.getMiddleName()))
                    .phoneNumber(StringUtils.defaultString(req.getPhoneNumber()))
                    .role(Role.DOCTOR)
                    .enabled(true)
                    .emailVerified(true)
                    .build());

            doctorProfileRepository.save(DoctorProfile.builder()
                    .userId(user.getId())
                    .specialization(req.getSpecialization())
                    .licenseNumber(req.getLicenseNumber())
                    .hospitalName(req.getHospitalName())
                    .department(req.getDepartment())
                    .yearsOfExperience(req.getYearsOfExperience())
                    .bio(req.getBio())
                    .education(req.getEducation())
                    .approved(false)
                    .build());

            notifyAdminsOfPendingDoctor(user);

            return RegisterResponse.builder()
                    .userId(user.getId())
                    .email(user.getEmail())
                    .role(Role.DOCTOR)
                    .approved(false)
                    .build();
        } catch (RuntimeException e) {
            log.error("Local DB write failed after Keycloak created {}, rolling back", userId, e);
            keycloak.deleteUserQuietly(userId);
            throw e;
        }
    }

    private void notifyAdminsOfPendingDoctor(User doctor) {
        List<User> admins = userRepository.findAllByRole(Role.ADMIN);
        if (admins.isEmpty()) return;
        Map<String, String> params = Map.of(
                "doctorName", doctor.getFullName(),
                "doctorEmail", doctor.getEmail());
        for (User admin : admins) {
            try {
                notificationPublisher.publish(
                        admin.getId(),
                        "DOCTOR_PENDING_APPROVAL",
                        doctor.getId().toString(),
                        "DOCTOR_PROFILE",
                        params);
            } catch (RuntimeException e) {
                log.warn("Failed to publish DOCTOR_PENDING_APPROVAL to admin {}: {}",
                        admin.getId(), e.getMessage());
            }
        }
    }

    private void ensureEmailFree(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email is already registered");
        }
    }

    public void forgotPassword(String email) {
        Optional<User> maybeUser = userRepository.findByEmail(email);
        if (maybeUser.isEmpty()) {
            log.info("forgot-password: no user found for {} (silently ignoring)", email);
            return;
        }

        User user = maybeUser.get();
        if (!user.isEnabled()) {
            log.info("forgot-password: user {} is disabled, skipping reset email", user.getId());
            return;
        }

        try {
            keycloak.sendExecuteActionsEmail(user.getId(), List.of("UPDATE_PASSWORD"));
            log.info("forgot-password: sent reset email to {}", email);
        } catch (RuntimeException e) {
            log.warn("forgot-password: failed to send reset email to {}: {}", email, e.getMessage());
        }
    }
}