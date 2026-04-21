package com.tynysai.userservice.controller;

import com.tynysai.userservice.dto.ApiResponse;
import com.tynysai.userservice.dto.PageResponse;
import com.tynysai.userservice.dto.request.CreateUserRequest;
import com.tynysai.userservice.dto.response.AdminStatsResponse;
import com.tynysai.userservice.dto.response.DoctorProfileResponse;
import com.tynysai.userservice.dto.response.UserResponse;
import com.tynysai.userservice.model.enums.Role;
import com.tynysai.userservice.service.AdminService;
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
public class AdminController {
    private final AdminService adminService;

    @GetMapping("/stats")
    public ApiResponse<AdminStatsResponse> getStats() {
        return ApiResponse.success(adminService.getStats());
    }

    @GetMapping("/users")
    public ApiResponse<PageResponse<UserResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ApiResponse.success(adminService.getAllUsers(pageable));
    }

    @GetMapping("/users/by-role")
    public ApiResponse<PageResponse<UserResponse>> getUsersByRole(
            @RequestParam Role role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ApiResponse.success(adminService.getUsersByRole(role, pageable));
    }

    @GetMapping("/users/search")
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
    public ApiResponse<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.success("User created", adminService.createUser(request));
    }

    @PatchMapping("/users/{userId}/toggle-status")
    public ApiResponse<UserResponse> toggleUserStatus(@PathVariable UUID userId) {
        return ApiResponse.success("User status updated", adminService.toggleUserStatus(userId));
    }

    @DeleteMapping("/users/{userId}")
    public ApiResponse<Void> deleteUser(@PathVariable UUID userId) {
        adminService.deleteUser(userId);
        return ApiResponse.success("User deleted");
    }

    @GetMapping("/doctors/pending")
    public ApiResponse<PageResponse<DoctorProfileResponse>> getPendingDoctors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size);
        return ApiResponse.success(adminService.getPendingDoctors(pageable));
    }

    @PatchMapping("/doctors/{doctorId}/approve")
    public ApiResponse<DoctorProfileResponse> approveDoctor(@PathVariable UUID doctorId) {
        return ApiResponse.success("Doctor approved", adminService.approveDoctor(doctorId));
    }

    @PatchMapping("/doctors/{doctorId}/reject")
    public ApiResponse<DoctorProfileResponse> rejectDoctor(@PathVariable UUID doctorId) {
        return ApiResponse.success("Doctor rejected", adminService.rejectDoctor(doctorId));
    }
}
