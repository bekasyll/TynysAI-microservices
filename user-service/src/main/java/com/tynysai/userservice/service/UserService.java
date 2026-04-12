package com.tynysai.userservice.service;

import com.tynysai.userservice.dto.request.CreateUserRequest;
import com.tynysai.userservice.dto.request.UpdateUserRequest;
import com.tynysai.userservice.dto.response.UserResponse;
import com.tynysai.userservice.exception.BadRequestException;
import com.tynysai.userservice.exception.ResourceNotFoundException;
import com.tynysai.userservice.mapper.UserMapper;
import com.tynysai.userservice.model.User;
import com.tynysai.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    public UserResponse getById(Long id) {
        return UserMapper.toUserResponse(findById(id));
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

        User user = UserMapper.fromCreateRequestToUser(request);
        return UserMapper.toUserResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse update(Long id, UpdateUserRequest request) {
        User user = findById(id);

        if (StringUtils.isNotBlank(request.getFirstName())) user.setFirstName(request.getFirstName());
        if (StringUtils.isNotBlank(request.getLastName())) user.setLastName(request.getLastName());
        if (StringUtils.isNotBlank(request.getMiddleName())) user.setMiddleName(request.getMiddleName());
        if (StringUtils.isNotBlank(request.getPhoneNumber())) user.setPhoneNumber(request.getPhoneNumber());
        return UserMapper.toUserResponse(userRepository.save(user));
    }

    @Transactional
    public void delete(Long id) {
        User user = findById(id);
        userRepository.delete(user);
    }

    @Transactional
    public UserResponse uploadAvatar(Long userId, MultipartFile file) {
        User user = findById(userId);
        String path = fileStorageService.storeAvatar(file, userId);
        user.setAvatarPath(path);
        return UserMapper.toUserResponse(userRepository.save(user));
    }

    @Transactional
    public void deleteAvatar(Long userId) {
        User user = findById(userId);
        if (user.getAvatarPath() != null) {
            fileStorageService.deleteFile(user.getAvatarPath());
            user.setAvatarPath(null);
            userRepository.save(user);
        }
    }

    @Transactional
    public UserResponse setEnabled(Long id, boolean enabled) {
        User user = findById(id);
        user.setEnabled(enabled);
        return UserMapper.toUserResponse(userRepository.save(user));
    }
}
