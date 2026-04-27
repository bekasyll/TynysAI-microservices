package com.tynysai.xrayservice.controller;

import com.tynysai.common.dto.ApiResponse;
import com.tynysai.common.dto.PageResponse;
import com.tynysai.xrayservice.dto.request.DoctorValidationRequest;
import com.tynysai.xrayservice.dto.response.XrayAnalysisResponse;
import com.tynysai.common.security.CurrentUserId;
import com.tynysai.xrayservice.service.XrayAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/xrays")
@RequiredArgsConstructor
@Tag(name = "Xray Analyses", description = "Загрузка снимков, AI-анализ (CNN NORMAL/PNEUMONIA), валидация врачом")
public class XrayAnalysisController {
    private final XrayAnalysisService xrayAnalysisService;

    @GetMapping("/{id}")
    @Operation(summary = "Анализ по ID", description = "С опциональным patientId для проверки владельца")
    public ApiResponse<XrayAnalysisResponse> getById(@PathVariable Long id,
                                                     @RequestParam(required = false) UUID patientId) {
        if (patientId != null) {
            return ApiResponse.success(xrayAnalysisService.getByIdForPatient(id, patientId));
        }
        return ApiResponse.success(xrayAnalysisService.getById(id));
    }

    @GetMapping("/patient")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Все рентген-анализы пациента")
    public ApiResponse<PageResponse<XrayAnalysisResponse>> getPatientAnalyses(
            @CurrentUserId UUID patientId,
            Pageable pageable) {
        return ApiResponse.success(xrayAnalysisService.getPatientAnalyses(patientId, pageable));
    }

    @GetMapping("/doctor/assigned")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "Анализы, назначенные врачу")
    public ApiResponse<PageResponse<XrayAnalysisResponse>> getDoctorAssigned(
            @CurrentUserId UUID doctorId,
            Pageable pageable) {
        return ApiResponse.success(xrayAnalysisService.getAssignedToDoctor(doctorId, pageable));
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Все рентген-анализы (admin)")
    public ApiResponse<PageResponse<XrayAnalysisResponse>> getAllAnalyses(Pageable pageable) {
        return ApiResponse.success(xrayAnalysisService.getAllAnalyses(pageable));
    }

    @GetMapping("/doctor/{id}")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "Анализ по ID (для назначенного врача)")
    public ApiResponse<XrayAnalysisResponse> getByIdForDoctor(@PathVariable Long id,
                                                              @CurrentUserId UUID doctorId) {
        return ApiResponse.success(xrayAnalysisService.getByIdForDoctor(id, doctorId));
    }

    @PostMapping(value = "/patient/upload", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Загрузить снимок (пациент)",
            description = "Запускает AI-анализ через Python-сервис, опционально назначает врача")
    public ApiResponse<XrayAnalysisResponse> uploadByPatient(
            @CurrentUserId UUID patientId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String patientNotes,
            @RequestParam(required = false) UUID assignedDoctorId) {
        return ApiResponse.success("Uploaded",
                xrayAnalysisService.uploadAndAnalyze(patientId, file, patientNotes, assignedDoctorId));
    }

    @PostMapping(value = "/doctor/upload", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "Загрузить снимок (врач)")
    public ApiResponse<XrayAnalysisResponse> uploadByDoctor(
            @CurrentUserId UUID doctorId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String notes) {
        return ApiResponse.success("Uploaded",
                xrayAnalysisService.uploadAndAnalyzeByDoctor(doctorId, file, notes));
    }

    @PostMapping("/{id}/validate")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "Валидировать AI-результат врачом",
            description = "Врач подтверждает или меняет AI-диагноз, добавляет заметки")
    public ApiResponse<XrayAnalysisResponse> validate(@PathVariable Long id,
                                                      @CurrentUserId UUID doctorId,
                                                      @Valid @RequestBody DoctorValidationRequest request) {
        return ApiResponse.success("Validated", xrayAnalysisService.validate(id, doctorId, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Удалить анализ")
    public ApiResponse<Void> delete(@PathVariable Long id, @CurrentUserId UUID patientId) {
        xrayAnalysisService.delete(id, patientId);
        return ApiResponse.success("Deleted", null);
    }
}
