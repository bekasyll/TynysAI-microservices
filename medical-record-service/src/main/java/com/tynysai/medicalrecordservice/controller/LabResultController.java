package com.tynysai.medicalrecordservice.controller;

import com.tynysai.common.dto.ApiResponse;
import com.tynysai.common.dto.PageResponse;
import com.tynysai.medicalrecordservice.dto.request.LabResultRequest;
import com.tynysai.medicalrecordservice.dto.response.LabResultResponse;
import com.tynysai.common.security.CurrentUserId;
import com.tynysai.medicalrecordservice.model.enums.LabTestType;
import com.tynysai.medicalrecordservice.service.LabResultService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/lab-results")
@RequiredArgsConstructor
@Tag(name = "Lab Results", description = "Лабораторные результаты пациента (общий анализ крови, биохимия, ПЦР и т.д.)")
public class LabResultController {
    private final LabResultService labResultService;

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    @Operation(summary = "Лабораторный результат по ID")
    public ApiResponse<LabResultResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(labResultService.getById(id));
    }

    @GetMapping("/patient/{id}")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Лабораторный результат по ID (для пациента)")
    public ApiResponse<LabResultResponse> getByIdForPatient(@PathVariable Long id,
                                                            @CurrentUserId UUID patientId) {
        return ApiResponse.success(labResultService.getByIdForPatient(id, patientId));
    }

    @GetMapping("/patient")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Все лабораторные результаты пациента (с фильтрами)")
    public ApiResponse<PageResponse<LabResultResponse>> getPatientLabResults(
            @CurrentUserId UUID patientId,
            @RequestParam(required = false) LabTestType testType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String q,
            Pageable pageable) {
        return ApiResponse.success(labResultService.getPatientLabResults(
                patientId, testType, from, to, q, pageable));
    }

    @GetMapping("/by-patient/{patientId}")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    @Operation(summary = "Лабораторные результаты конкретного пациента (для доктора/админа)",
            description = "Тот же фильтр-набор, что и /patient, но patientId явный — врач видит " +
                    "анализы пациента, которому он прописывал/которого ведёт.")
    public ApiResponse<PageResponse<LabResultResponse>> getLabResultsByPatientId(
            @PathVariable UUID patientId,
            @RequestParam(required = false) LabTestType testType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String q,
            Pageable pageable) {
        return ApiResponse.success(labResultService.getPatientLabResults(
                patientId, testType, from, to, q, pageable));
    }

    @PostMapping
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "Добавить лабораторный результат")
    public ApiResponse<LabResultResponse> create(@CurrentUserId UUID doctorId,
                                                 @Valid @RequestBody LabResultRequest request) {
        return ApiResponse.success("Lab result created", labResultService.create(doctorId, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    @Operation(summary = "Удалить лабораторный результат")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        labResultService.delete(id);
        return ApiResponse.success("Deleted", null);
    }
}
