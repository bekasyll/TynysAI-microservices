package com.tynysai.userservice.controller;

import com.tynysai.common.dto.ApiResponse;
import com.tynysai.common.dto.PageResponse;
import com.tynysai.userservice.dto.request.AdminResetPasswordRequest;
import com.tynysai.userservice.dto.request.RegisterDoctorRequest;
import com.tynysai.userservice.dto.request.RegisterPatientRequest;
import com.tynysai.userservice.dto.response.AdminStatsResponse;
import com.tynysai.userservice.dto.response.DoctorProfileResponse;
import com.tynysai.userservice.dto.response.RegisterResponse;
import com.tynysai.userservice.dto.response.UserResponse;
import com.tynysai.userservice.model.enums.Role;
import com.tynysai.userservice.service.AdminService;
import com.tynysai.userservice.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Администрирование пользователей и одобрение врачей.")
public class AdminController {
    private final AdminService adminService;
    private final AuthService authService;

    @GetMapping("/stats")
    @Operation(summary = "Сводная статистика по пользователям")
    public ApiResponse<AdminStatsResponse> getStats() {
        return ApiResponse.success(adminService.getStats());
    }

    @GetMapping("/users")
    @Operation(summary = "Список всех пользователей (пагинация)")
    public ApiResponse<PageResponse<UserResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ApiResponse.success(adminService.getAllUsers(pageable));
    }

    @GetMapping("/users/by-role")
    @Operation(summary = "Список пользователей по роли (PATIENT/DOCTOR/ADMIN)")
    public ApiResponse<PageResponse<UserResponse>> getUsersByRole(
            @RequestParam Role role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ApiResponse.success(adminService.getUsersByRole(role, pageable));
    }

    @GetMapping("/users/search")
    @Operation(summary = "Поиск пользователей по имени/email")
    public ApiResponse<PageResponse<UserResponse>> searchUsers(
            @RequestParam(required = false) Role role,
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size);
        return ApiResponse.success(adminService.searchUsers(role, query, pageable));
    }

    @PostMapping("/users/patient")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать пациента (email сразу подтверждён, пароль временный)")
    public ApiResponse<RegisterResponse> createPatient(@Valid @RequestBody RegisterPatientRequest request) {
        return ApiResponse.success("Patient created",
                authService.registerPatient(request, /* adminInitiated */ true));
    }

    @PostMapping("/users/doctor")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать врача (auto-approved, email подтверждён, пароль временный)")
    public ApiResponse<RegisterResponse> createDoctor(@Valid @RequestBody RegisterDoctorRequest request) {
        return ApiResponse.success("Doctor created and approved",
                authService.registerDoctor(request, /* adminInitiated */ true));
    }

    @PatchMapping("/users/{userId}/toggle-status")
    @Operation(summary = "Заблокировать/разблокировать пользователя (с прокидыванием в Keycloak)")
    public ApiResponse<UserResponse> toggleUserStatus(@PathVariable UUID userId) {
        return ApiResponse.success("User status updated", adminService.toggleStatus(userId));
    }

    @DeleteMapping("/users/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Удалить пользователя из Keycloak и локальной БД")
    public ApiResponse<Void> deleteUser(@PathVariable UUID userId) {
        adminService.deleteUser(userId);
        return ApiResponse.success("User deleted");
    }

    @PostMapping("/users/{userId}/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Сбросить пароль пользователя (по умолчанию - временный)")
    public ApiResponse<Void> resetPassword(@PathVariable UUID userId,
                                           @Valid @RequestBody AdminResetPasswordRequest request) {
        adminService.resetPassword(userId, request.getNewPassword(), request.isTemporary());
        return ApiResponse.success("Password reset");
    }

    @PostMapping("/users/{userId}/send-verify-email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Отправить пользователю письмо с подтверждением email")
    public ApiResponse<Void> sendVerifyEmail(@PathVariable UUID userId) {
        adminService.sendVerifyEmail(userId);
        return ApiResponse.success("Verification email sent");
    }

    @PostMapping("/users/{userId}/logout-sessions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Завершить все активные сессии пользователя")
    public ApiResponse<Void> logoutSessions(@PathVariable UUID userId) {
        adminService.logoutSessions(userId);
        return ApiResponse.success("Sessions revoked");
    }

    @GetMapping("/doctors/pending")
    @Operation(summary = "Список врачей, ожидающих одобрения")
    public ApiResponse<PageResponse<DoctorProfileResponse>> getPendingDoctors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size);
        return ApiResponse.success(adminService.getPendingDoctors(pageable));
    }

    @PatchMapping("/doctors/{doctorId}/approve")
    @Operation(summary = "Одобрить заявку врача")
    public ApiResponse<DoctorProfileResponse> approveDoctor(@PathVariable UUID doctorId) {
        return ApiResponse.success("Doctor approved", adminService.approveDoctor(doctorId));
    }

    @PatchMapping("/doctors/{doctorId}/reject")
    @Operation(summary = "Отклонить заявку врача")
    public ApiResponse<DoctorProfileResponse> rejectDoctor(@PathVariable UUID doctorId) {
        return ApiResponse.success("Doctor rejected", adminService.rejectDoctor(doctorId));
    }
}
