package com.tynysai.userservice.controller;

import com.tynysai.userservice.dto.ApiResponse;
import com.tynysai.userservice.dto.PageResponse;
import com.tynysai.userservice.dto.request.UpdateDoctorProfileRequest;
import com.tynysai.userservice.dto.response.DoctorProfileResponse;
import com.tynysai.userservice.security.CurrentUserId;
import com.tynysai.userservice.service.DoctorProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
@Tag(name = "Doctors", description = "Профили врачей: специализация, лицензия, расписание")
public class DoctorController {
    private final DoctorProfileService doctorProfileService;

    @GetMapping("/me")
    @Operation(summary = "Профиль текущего врача")
    public ApiResponse<DoctorProfileResponse> getMyProfile(@CurrentUserId UUID userId) {
        return ApiResponse.success(doctorProfileService.getMyProfile(userId));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Профиль врача по userId")
    public ApiResponse<DoctorProfileResponse> getByUserId(@PathVariable UUID userId) {
        return ApiResponse.success(doctorProfileService.getByUserId(userId));
    }

    @GetMapping("/approved")
    @Operation(summary = "Список одобренных врачей (пагинация)")
    public ApiResponse<PageResponse<DoctorProfileResponse>> listApprovedDoctors(Pageable pageable) {
        return ApiResponse.success(doctorProfileService.listApproved(pageable));
    }

    @PutMapping("/me")
    @Operation(summary = "Обновить профиль врача")
    public ApiResponse<DoctorProfileResponse> updateMyProfile(
            @CurrentUserId UUID userId,
            @Valid @RequestBody UpdateDoctorProfileRequest request) {
        return ApiResponse.success("Doctor profile updated", doctorProfileService.updateMyProfile(userId, request));
    }
}
