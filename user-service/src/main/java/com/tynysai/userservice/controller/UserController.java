package com.tynysai.userservice.controller;

import com.tynysai.userservice.dto.ApiResponse;
import com.tynysai.userservice.dto.request.CreateUserRequest;
import com.tynysai.userservice.dto.request.UpdateUserRequest;
import com.tynysai.userservice.dto.response.UserResponse;
import com.tynysai.userservice.model.User;
import com.tynysai.userservice.service.FileStorageService;
import com.tynysai.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final FileStorageService fileStorageService;

    @GetMapping("/me")
    public ApiResponse<UserResponse> getCurrentUser(@RequestHeader("X-User-Id") Long id) {
        return ApiResponse.success(userService.getById(id));
    }

    @GetMapping("/{id}")
    public ApiResponse<UserResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(userService.getById(id));
    }

    @GetMapping(params = "email")
    public ApiResponse<UserResponse> getByEmail(@RequestParam String email) {
        return ApiResponse.success(userService.getByEmail(email));
    }

    @PostMapping
    public ApiResponse<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.success("User created", userService.createUser(request));
    }

    @PutMapping("/me")
    public ApiResponse<UserResponse> updateCurrentUser(@RequestHeader("X-User-Id") Long id,
                                                       @Valid @RequestBody UpdateUserRequest request) {
        return ApiResponse.success("User updated", userService.update(id, request));
    }

    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UserResponse> uploadAvatar(@RequestHeader("X-User-Id") Long userId,
                                                  @RequestPart("file") MultipartFile file) {
        return ApiResponse.success("Avatar uploaded", userService.uploadAvatar(userId, file));
    }

    @GetMapping("/{userId}/avatar")
    public ResponseEntity<byte[]> getAvatar(@PathVariable Long userId) {
        User user = userService.findById(userId);
        if (user.getAvatarPath() == null) {
            return ResponseEntity.notFound().build();
        }
        byte[] data = fileStorageService.loadFile(user.getAvatarPath());
        String ext = user.getAvatarPath().substring(user.getAvatarPath().lastIndexOf('.') + 1).toLowerCase();
        MediaType mediaType = ext.equals("png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok().contentType(mediaType).body(data);
    }

    @DeleteMapping("/me/avatar")
    public ApiResponse<Void> deleteAvatar(@RequestHeader("X-User-Id") Long userId) {
        userService.deleteAvatar(userId);
        return ApiResponse.success("Avatar deleted");
    }
}
