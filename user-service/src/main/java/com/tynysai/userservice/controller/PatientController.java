package com.tynysai.userservice.controller;

import com.tynysai.userservice.dto.ApiResponse;
import com.tynysai.userservice.dto.PageResponse;
import com.tynysai.userservice.dto.request.UpdatePatientProfileRequest;
import com.tynysai.userservice.dto.response.PatientProfileResponse;
import com.tynysai.userservice.service.PatientProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
public class PatientController {
    private final PatientProfileService patientProfileService;

    @GetMapping("/me")
    public ApiResponse<PatientProfileResponse> getMyProfile(@RequestHeader("X-User-Id") UUID userId) {
        return ApiResponse.success(patientProfileService.getMyProfile(userId));
    }

    @GetMapping("/{userId}")
    public ApiResponse<PatientProfileResponse> getByUserId(@PathVariable UUID userId) {
        return ApiResponse.success(patientProfileService.getByUserId(userId));
    }

    @GetMapping
    public ApiResponse<PageResponse<PatientProfileResponse>> patientsList(Pageable pageable) {
        return ApiResponse.success(patientProfileService.listPatients(pageable));
    }

    @PutMapping("/me")
    public ApiResponse<PatientProfileResponse> updateMyProfile(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody UpdatePatientProfileRequest request) {
        return ApiResponse.success("Patient profile updated", patientProfileService.updateMyProfile(userId, request));
    }
}
