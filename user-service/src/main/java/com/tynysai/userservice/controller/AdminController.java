package com.tynysai.userservice.controller;

import com.tynysai.common.dto.ApiResponse;
import com.tynysai.common.dto.PageResponse;
import com.tynysai.userservice.dto.request.RegisterDoctorRequest;
import com.tynysai.userservice.dto.request.RegisterPatientRequest;
import com.tynysai.userservice.dto.request.UpdateWorkScheduleRequest;
import com.tynysai.userservice.dto.response.*;
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
import org.springframework.web.bind.annotation.*;

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
                authService.registerPatient(request));
    }

    @PostMapping("/users/doctor")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать врача (email подтверждён, пароль временный, одобрение - вручную)",
            description = "Профиль врача создаётся с approved=false. Все админы получают " +
                    "уведомление DOCTOR_PENDING_APPROVAL и должны подтвердить заявку через " +
                    "PATCH /api/admin/doctors/{id}/approve.")
    public ApiResponse<RegisterResponse> createDoctor(@Valid @RequestBody RegisterDoctorRequest request) {
        return ApiResponse.success("Doctor created, awaiting approval",
                authService.registerDoctor(request));
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

    @PostMapping("/users/{userId}/logout-sessions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Завершить все активные сессии пользователя")
    public ApiResponse<Void> logoutSessions(@PathVariable UUID userId) {
        adminService.logoutSessions(userId);
        return ApiResponse.success("Sessions revoked");
    }

    @GetMapping("/doctors/pending")
    @Operation(summary = "Список врачей, ожидающих одобрения (с поиском)",
            description = "q ищет по ФИО/email доктора, специализации, лицензии, клинике и отделу.")
    public ApiResponse<PageResponse<DoctorProfileResponse>> getPendingDoctors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false, name = "q") String query) {
        PageRequest pageable = PageRequest.of(page, size);
        return ApiResponse.success(adminService.getPendingDoctors(query, pageable));
    }

    @PatchMapping("/doctors/{doctorId}/approve")
    @Operation(summary = "Одобрить заявку врача")
    public ApiResponse<DoctorProfileResponse> approveDoctor(@PathVariable UUID doctorId) {
        return ApiResponse.success("Doctor approved", adminService.approveDoctor(doctorId));
    }

    @PatchMapping("/doctors/{doctorId}/reject")
    @Operation(summary = "Отклонить заявку врача (полное удаление учётки)",
            description = "Стирает запись доктора и в Keycloak, и в БД. " +
                    "Email после этого можно зарегистрировать заново.")
    public ApiResponse<Void> rejectDoctor(@PathVariable UUID doctorId) {
        adminService.deleteUser(doctorId);
        return ApiResponse.success("Doctor rejected");
    }

    @PutMapping("/doctors/{doctorId}/work-schedule")
    @Operation(summary = "Обновить рабочее расписание доктора",
            description = "Принимает Map<DayOfWeek, List<TimeRange>>. Интервалы внутри " +
                    "одного дня не должны пересекаться, end > start.")
    public ApiResponse<DoctorProfileResponse> updateDoctorWorkSchedule(
            @PathVariable UUID doctorId,
            @Valid @RequestBody UpdateWorkScheduleRequest request) {
        return ApiResponse.success("Work schedule updated",
                adminService.updateDoctorWorkSchedule(doctorId, request.getWorkSchedule()));
    }
}
