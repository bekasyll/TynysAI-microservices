package com.tynysai.userservice.service;

import com.tynysai.userservice.dto.request.ChangePasswordRequest;
import com.tynysai.userservice.dto.request.UpdateUserRequest;
import com.tynysai.userservice.dto.response.UserResponse;
import com.tynysai.userservice.exception.BadRequestException;
import com.tynysai.userservice.exception.ResourceNotFoundException;
import com.tynysai.userservice.mapper.UserMapper;
import com.tynysai.userservice.model.User;
import com.tynysai.userservice.model.enums.Role;
import com.tynysai.userservice.repository.UserRepository;
import com.tynysai.userservice.security.KeycloakAdminClient;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final KeycloakAdminClient keycloak;

    public User findById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    public UserResponse getById(UUID id) {
        return UserMapper.toUserResponse(findById(id));
    }

    @Transactional
    public UserResponse getOrProvision(UUID id, String email, String firstName, String lastName,
                                       Collection<String> realmRoles) {
        return userRepository.findById(id)
                .map(UserMapper::toUserResponse)
                .orElseGet(() -> UserMapper.toUserResponse(userRepository.save(
                        User.builder()
                                .id(id)
                                .email(StringUtils.defaultIfBlank(email, id.toString()))
                                .firstName(StringUtils.defaultString(firstName, ""))
                                .lastName(StringUtils.defaultString(lastName, ""))
                                .phoneNumber("")
                                .role(resolveRole(realmRoles))
                                .enabled(true)
                                .emailVerified(true)
                                .build())));
    }

    private Role resolveRole(Collection<String> roles) {
        if (roles == null || roles.isEmpty()) return Role.PATIENT;
        if (roles.stream().anyMatch(r -> r.equalsIgnoreCase("ADMIN"))) return Role.ADMIN;
        if (roles.stream().anyMatch(r -> r.equalsIgnoreCase("DOCTOR"))) return Role.DOCTOR;
        return Role.PATIENT;
    }

    public UserResponse getByEmail(String email) {
        return UserMapper.toUserResponse(userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email)));
    }

    @Transactional
    public UserResponse update(UUID id, UpdateUserRequest request) {
        User user = findById(id);

        if (StringUtils.isNotBlank(request.getFirstName())) user.setFirstName(request.getFirstName());
        if (StringUtils.isNotBlank(request.getLastName())) user.setLastName(request.getLastName());
        if (StringUtils.isNotBlank(request.getMiddleName())) user.setMiddleName(request.getMiddleName());
        if (StringUtils.isNotBlank(request.getPhoneNumber())) user.setPhoneNumber(request.getPhoneNumber());
        return UserMapper.toUserResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse uploadAvatar(UUID userId, MultipartFile file) {
        User user = findById(userId);
        String path = fileStorageService.storeAvatar(file, userId);
        user.setAvatarPath(path);
        return UserMapper.toUserResponse(userRepository.save(user));
    }

    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = findById(userId);
        if (!keycloak.verifyPassword(user.getEmail(), request.getCurrentPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }
        keycloak.resetPassword(userId, request.getNewPassword(), false);
    }

    @Transactional
    public void deleteAvatar(UUID userId) {
        User user = findById(userId);
        if (user.getAvatarPath() != null) {
            fileStorageService.deleteFile(user.getAvatarPath());
            user.setAvatarPath(null);
            userRepository.save(user);
        }
    }

}
