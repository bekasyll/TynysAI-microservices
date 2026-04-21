package com.tynysai.userservice.controller;

import com.tynysai.userservice.dto.ApiResponse;
import com.tynysai.userservice.dto.PageResponse;
import com.tynysai.userservice.dto.request.UpdateDoctorProfileRequest;
import com.tynysai.userservice.dto.response.DoctorProfileResponse;
import com.tynysai.userservice.service.DoctorProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
public class DoctorController {
    private final DoctorProfileService doctorProfileService;

    @GetMapping("/me")
    public ApiResponse<DoctorProfileResponse> getMyProfile(@RequestHeader("X-User-Id") UUID userId) {
        return ApiResponse.success(doctorProfileService.getMyProfile(userId));
    }

    @GetMapping("/{userId}")
    public ApiResponse<DoctorProfileResponse> getByUserId(@PathVariable UUID userId) {
        return ApiResponse.success(doctorProfileService.getByUserId(userId));
    }

    @GetMapping("/approved")
    public ApiResponse<PageResponse<DoctorProfileResponse>> listApprovedDoctors(Pageable pageable) {
        return ApiResponse.success(doctorProfileService.listApproved(pageable));
    }

    @PutMapping("/me")
    public ApiResponse<DoctorProfileResponse> updateMyProfile(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody UpdateDoctorProfileRequest request) {
        return ApiResponse.success("Doctor profile updated", doctorProfileService.updateMyProfile(userId, request));
    }
}
