package com.tynysai.userservice.service;

import com.tynysai.userservice.dto.PageResponse;
import com.tynysai.userservice.dto.request.CreateUserRequest;
import com.tynysai.userservice.dto.response.AdminStatsResponse;
import com.tynysai.userservice.dto.response.DoctorProfileResponse;
import com.tynysai.userservice.dto.response.UserResponse;
import com.tynysai.userservice.mapper.UserMapper;
import com.tynysai.userservice.model.User;
import com.tynysai.userservice.model.enums.Role;
import com.tynysai.userservice.repository.DoctorProfileRepository;
import com.tynysai.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {
    private final UserRepository userRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final UserService userService;
    private final DoctorProfileService doctorProfileService;

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

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        return userService.createUser(request);
    }

    @Transactional
    public UserResponse toggleUserStatus(Long userId) {
        User user = userService.findById(userId);
        user.setEnabled(!user.isEnabled());
        return UserMapper.toUserResponse(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(Long userId) {
        userService.delete(userId);
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
    public DoctorProfileResponse approveDoctor(Long doctorUserId) {
        return doctorProfileService.approve(doctorUserId, true);
    }

    @Transactional
    public DoctorProfileResponse rejectDoctor(Long doctorUserId) {
        return doctorProfileService.approve(doctorUserId, false);
    }
}
