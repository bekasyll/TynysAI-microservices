package com.tynysai.userservice.controller;

import com.tynysai.userservice.dto.ApiResponse;
import com.tynysai.userservice.dto.PageResponse;
import com.tynysai.userservice.dto.request.UpdatePatientProfileRequest;
import com.tynysai.userservice.dto.response.PatientProfileResponse;
import com.tynysai.userservice.security.CurrentUserId;
import com.tynysai.userservice.service.PatientProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
@Tag(name = "Patients", description = "Профили пациентов: рост, вес, аллергии, экстренный контакт")
public class PatientController {
    private final PatientProfileService patientProfileService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Профиль текущего пациента")
    public ApiResponse<PatientProfileResponse> getMyProfile(@CurrentUserId UUID userId) {
        return ApiResponse.success(patientProfileService.getMyProfile(userId));
    }

    @GetMapping("/{userId}")
    @PreAuthorize("@doctorAuthorizer.canViewPatients(authentication)")
    @Operation(summary = "Профиль пациента по userId",
            description = "Доступно ADMIN и одобренным DOCTOR'ам")
    public ApiResponse<PatientProfileResponse> getByUserId(@PathVariable UUID userId) {
        return ApiResponse.success(patientProfileService.getByUserId(userId));
    }

    @GetMapping
    @PreAuthorize("@doctorAuthorizer.canViewPatients(authentication)")
    @Operation(summary = "Список пациентов (пагинация)",
            description = "Доступно ADMIN и одобренным DOCTOR'ам")
    public ApiResponse<PageResponse<PatientProfileResponse>> patientsList(Pageable pageable) {
        return ApiResponse.success(patientProfileService.listPatients(pageable));
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Обновить профиль пациента")
    public ApiResponse<PatientProfileResponse> updateMyProfile(
            @CurrentUserId UUID userId,
            @Valid @RequestBody UpdatePatientProfileRequest request) {
        return ApiResponse.success("Patient profile updated", patientProfileService.updateMyProfile(userId, request));
    }
}
