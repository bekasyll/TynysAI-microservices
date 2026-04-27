package com.tynysai.userservice.controller;

import com.tynysai.common.dto.ApiResponse;
import com.tynysai.userservice.dto.request.ChangePasswordRequest;
import com.tynysai.userservice.dto.request.UpdateUserRequest;
import com.tynysai.userservice.dto.response.UserResponse;
import com.tynysai.userservice.model.User;
import com.tynysai.common.security.CurrentUserId;
import com.tynysai.userservice.service.FileStorageService;
import com.tynysai.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Профили пользователей и аватары")
public class UserController {
    private final UserService userService;
    private final FileStorageService fileStorageService;

    @GetMapping("/me")
    @Operation(summary = "Получить текущего пользователя",
            description = "Возвращает или авто-создаёт пользователя по claim'ам валидированного JWT (Keycloak)")
    public ApiResponse<UserResponse> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        UUID id = UUID.fromString(jwt.getSubject());
        String email = jwt.getClaimAsString("email");
        String firstName = jwt.getClaimAsString("given_name");
        String lastName = jwt.getClaimAsString("family_name");

        Collection<String> roles = extractRealmRoles(jwt);
        return ApiResponse.success(userService.getOrProvision(id, email, firstName, lastName, roles));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить пользователя по ID")
    public ApiResponse<UserResponse> getById(@PathVariable UUID id) {
        return ApiResponse.success(userService.getById(id));
    }

    @GetMapping(params = "email")
    @Operation(summary = "Найти пользователя по email")
    public ApiResponse<UserResponse> getByEmail(@RequestParam String email) {
        return ApiResponse.success(userService.getByEmail(email));
    }

    @PutMapping("/me")
    @Operation(summary = "Обновить профиль текущего пользователя")
    public ApiResponse<UserResponse> updateCurrentUser(@CurrentUserId UUID id,
                                                       @Valid @RequestBody UpdateUserRequest request) {
        return ApiResponse.success("User updated", userService.update(id, request));
    }

    @PutMapping("/me/password")
    @Operation(summary = "Сменить пароль текущего пользователя",
            description = "Проверяет старый пароль через Keycloak password grant, затем " +
                    "обновляет на новый. Сбрасывает все активные сессии при смене.")
    public ApiResponse<Void> changePassword(@CurrentUserId UUID userId,
                                            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(userId, request);
        return ApiResponse.success("Password changed");
    }

    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Загрузить аватар", description = "JPEG/PNG до 5 МБ")
    public ApiResponse<UserResponse> uploadAvatar(@CurrentUserId UUID userId,
                                                  @RequestPart("file") MultipartFile file) {
        return ApiResponse.success("Avatar uploaded", userService.uploadAvatar(userId, file));
    }

    @GetMapping("/{userId}/avatar")
    @Operation(summary = "Скачать аватар пользователя")
    public ResponseEntity<byte[]> getAvatar(@PathVariable UUID userId) {
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
    @Operation(summary = "Удалить аватар текущего пользователя")
    public ApiResponse<Void> deleteAvatar(@CurrentUserId UUID userId) {
        userService.deleteAvatar(userId);
        return ApiResponse.success("Avatar deleted");
    }

    @SuppressWarnings("unchecked")
    private static Collection<String> extractRealmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess == null) return List.of();
        Object roles = realmAccess.get("roles");
        return roles instanceof Collection<?> c
                ? c.stream().filter(r -> r instanceof String).map(Object::toString).toList()
                : List.of();
    }
}
