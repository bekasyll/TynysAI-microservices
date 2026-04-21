package com.tynysai.userservice.service;

import com.tynysai.userservice.dto.request.CreateUserRequest;
import com.tynysai.userservice.dto.request.RegisterRequest;
import com.tynysai.userservice.dto.request.UpdateUserRequest;
import com.tynysai.userservice.dto.response.UserResponse;
import com.tynysai.userservice.exception.BadRequestException;
import com.tynysai.userservice.exception.ResourceNotFoundException;
import com.tynysai.userservice.mapper.UserMapper;
import com.tynysai.userservice.model.User;
import com.tynysai.userservice.model.enums.Role;
import com.tynysai.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final KeycloakAdminService keycloakAdminService;

    public User findById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    public UserResponse getById(UUID id) {
        return UserMapper.toUserResponse(findById(id));
    }

    @Transactional
    public UserResponse registerPatient(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered");
        }

        UUID keycloakId = keycloakAdminService.createUser(
                request.getEmail(),
                request.getPassword(),
                request.getFirstName(),
                request.getLastName());

        try {
            User user = User.builder()
                    .id(keycloakId)
                    .email(request.getEmail())
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .phoneNumber(StringUtils.defaultString(request.getPhoneNumber(), ""))
                    .role(Role.PATIENT)
                    .enabled(true)
                    .emailVerified(true)
                    .build();
            return UserMapper.toUserResponse(userRepository.saveAndFlush(user));
        } catch (RuntimeException e) {
            keycloakAdminService.deleteUserQuietly(keycloakId);
            throw e;
        }
    }

    @Transactional
    public UserResponse getOrProvision(UUID id, String email, String rolesHeader) {
        return userRepository.findById(id)
                .map(UserMapper::toUserResponse)
                .orElseGet(() -> UserMapper.toUserResponse(userRepository.save(
                        User.builder()
                                .id(id)
                                .email(StringUtils.defaultIfBlank(email, id.toString()))
                                .firstName("")
                                .lastName("")
                                .phoneNumber("")
                                .role(resolveRole(rolesHeader))
                                .enabled(true)
                                .emailVerified(true)
                                .build())));
    }

    private Role resolveRole(String rolesHeader) {
        if (StringUtils.isBlank(rolesHeader)) return Role.PATIENT;
        Set<String> roles = Arrays.stream(rolesHeader.split(","))
                .map(String::trim)
                .map(String::toUpperCase)
                .collect(Collectors.toSet());
        if (roles.contains("ADMIN")) return Role.ADMIN;
        if (roles.contains("DOCTOR")) return Role.DOCTOR;
        return Role.PATIENT;
    }

    public UserResponse getByEmail(String email) {
        return UserMapper.toUserResponse(userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email)));
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered");
        }

        UUID keycloakId = keycloakAdminService.createUserWithRole(
                request.getEmail(),
                request.getPassword(),
                request.getFirstName(),
                request.getLastName(),
                request.getRole().name());

        try {
            User user = User.builder()
                    .id(keycloakId)
                    .email(request.getEmail())
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .middleName(request.getMiddleName())
                    .phoneNumber(StringUtils.defaultString(request.getPhoneNumber(), ""))
                    .role(request.getRole())
                    .enabled(true)
                    .emailVerified(true)
                    .build();
            return UserMapper.toUserResponse(userRepository.saveAndFlush(user));
        } catch (RuntimeException e) {
            keycloakAdminService.deleteUserQuietly(keycloakId);
            throw e;
        }
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
    public void delete(UUID id) {
        User user = findById(id);
        userRepository.delete(user);
    }

    @Transactional
    public UserResponse uploadAvatar(UUID userId, MultipartFile file) {
        User user = findById(userId);
        String path = fileStorageService.storeAvatar(file, userId);
        user.setAvatarPath(path);
        return UserMapper.toUserResponse(userRepository.save(user));
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

    @Transactional
    public UserResponse setEnabled(UUID id, boolean enabled) {
        User user = findById(id);
        user.setEnabled(enabled);
        return UserMapper.toUserResponse(userRepository.save(user));
    }
}
