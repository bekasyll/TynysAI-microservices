package com.tynysai.userservice.service;

import com.tynysai.common.dto.PageResponse;
import com.tynysai.userservice.dto.response.AdminStatsResponse;
import com.tynysai.userservice.dto.response.DoctorProfileResponse;
import com.tynysai.userservice.dto.response.UserResponse;
import com.tynysai.userservice.exception.BadRequestException;
import com.tynysai.userservice.mapper.UserMapper;
import com.tynysai.userservice.model.User;
import com.tynysai.userservice.model.enums.Role;
import com.tynysai.userservice.repository.DoctorProfileRepository;
import com.tynysai.userservice.repository.PatientProfileRepository;
import com.tynysai.userservice.repository.UserRepository;
import com.tynysai.userservice.security.KeycloakAdminClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AdminService {
    private final UserRepository userRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final UserService userService;
    private final DoctorProfileService doctorProfileService;
    private final KeycloakAdminClient keycloak;

    public AdminStatsResponse getStats() {
        return AdminStatsResponse.builder()
                .totalUsers(userRepository.count())
                .totalPatients(userRepository.countByRole(Role.PATIENT))
                .totalDoctors(userRepository.countByRole(Role.DOCTOR))
                .activePatients(userRepository.countByRoleAndEnabled(Role.PATIENT, true))
                .activeDoctors(userRepository.countByRoleAndEnabled(Role.DOCTOR, true))
                .pendingDoctorApprovals(doctorProfileRepository.countByApproved(false))
                .build();
    }

    public PageResponse<UserResponse> getAllUsers(Pageable pageable) {
        Page<User> page = userRepository.findAll(pageable);
        return PageResponse.from(page.map(UserMapper::toUserResponse));
    }

    public PageResponse<UserResponse> getUsersByRole(Role role, Pageable pageable) {
        Page<User> page = userRepository.findByRole(role, pageable);
        return PageResponse.from(page.map(UserMapper::toUserResponse));
    }

    public PageResponse<UserResponse> searchUsers(Role role, String query, Pageable pageable) {
        Page<User> page;
        if (role != null) {
            page = userRepository.searchByRoleAndName(role, query, pageable);
        } else {
            page = userRepository.searchByName(query, pageable);
        }
        return PageResponse.from(page.map(UserMapper::toUserResponse));
    }

    public PageResponse<DoctorProfileResponse> getPendingDoctors(Pageable pageable) {
        return PageResponse.from(
                doctorProfileRepository.findByApproved(false, pageable)
                        .map(d -> {
                            User user = userService.findById(d.getUserId());
                            return UserMapper.toDoctorResponse(d, user);
                        }));
    }

    @Transactional
    public DoctorProfileResponse approveDoctor(UUID doctorUserId) {
        return doctorProfileService.approve(doctorUserId, true);
    }

    @Transactional
    public DoctorProfileResponse rejectDoctor(UUID doctorUserId) {
        return doctorProfileService.approve(doctorUserId, false);
    }

    @Transactional
    public UserResponse toggleStatus(UUID userId) {
        User user = userService.findById(userId);
        boolean nextEnabled = !user.isEnabled();
        keycloak.setUserEnabled(userId, nextEnabled);
        user.setEnabled(nextEnabled);
        return UserMapper.toUserResponse(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(UUID userId) {
        User user = userService.findById(userId);
        if (user.getRole() == Role.ADMIN) {
            throw new BadRequestException("Admin accounts must be removed manually in Keycloak");
        }
        try {
            keycloak.deleteUser(userId);
        } catch (HttpClientErrorException.NotFound ignored) {
            // already gone in Keycloak - proceed with local cleanup
        }
        doctorProfileRepository.deleteByUserId(userId);
        patientProfileRepository.deleteByUserId(userId);
        userRepository.delete(user);
        log.info("Admin removed user {} ({})", user.getEmail(), userId);
    }

    public void resetPassword(UUID userId, String newPassword, boolean temporary) {
        userService.findById(userId);
        keycloak.resetPassword(userId, newPassword, temporary);
        log.info("Admin reset password for user {} (temporary={})", userId, temporary);
    }

    public void sendVerifyEmail(UUID userId) {
        userService.findById(userId);
        try {
            keycloak.sendVerifyEmail(userId);
        } catch (HttpClientErrorException e) {
            throw new BadRequestException("Keycloak could not send the email. " +
                    "Check Realm settings → Email (SMTP).");
        }
    }

    public void logoutSessions(UUID userId) {
        userService.findById(userId);
        keycloak.logoutAllSessions(userId);
        log.info("Admin revoked all sessions for user {}", userId);
    }
}
