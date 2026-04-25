package com.tynysai.userservice.controller;

import com.tynysai.userservice.dto.ApiResponse;
import com.tynysai.userservice.dto.PageResponse;
import com.tynysai.userservice.dto.request.CreateUserRequest;
import com.tynysai.userservice.dto.response.AdminStatsResponse;
import com.tynysai.userservice.dto.response.DoctorProfileResponse;
import com.tynysai.userservice.dto.response.UserResponse;
import com.tynysai.userservice.model.enums.Role;
import com.tynysai.userservice.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Администрирование пользователей и одобрение врачей")
public class AdminController {
    private final AdminService adminService;

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

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать пользователя", description = "Создаёт пользователя в Keycloak и БД с любой ролью")
    public ApiResponse<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.success("User created", adminService.createUser(request));
    }

    @PatchMapping("/users/{userId}/toggle-status")
    @Operation(summary = "Включить/выключить пользователя")
    public ApiResponse<UserResponse> toggleUserStatus(@PathVariable UUID userId) {
        return ApiResponse.success("User status updated", adminService.toggleUserStatus(userId));
    }

    @DeleteMapping("/users/{userId}")
    @Operation(summary = "Удалить пользователя из Keycloak и БД")
    public ApiResponse<Void> deleteUser(@PathVariable UUID userId) {
        adminService.deleteUser(userId);
        return ApiResponse.success("User deleted");
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
