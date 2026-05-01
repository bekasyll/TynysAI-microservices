package com.tynysai.medicalrecordservice.controller;

import com.tynysai.common.dto.ApiResponse;
import com.tynysai.common.dto.PageResponse;
import com.tynysai.medicalrecordservice.dto.request.DiagnosticReportRequest;
import com.tynysai.medicalrecordservice.dto.response.DiagnosticReportResponse;
import com.tynysai.common.security.CurrentUserId;
import com.tynysai.medicalrecordservice.model.enums.DiseaseType;
import com.tynysai.medicalrecordservice.model.enums.Severity;
import com.tynysai.medicalrecordservice.service.DiagnosticReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Diagnostic Reports", description = "Диагностические заключения врачей")
public class DiagnosticReportController {
    private final DiagnosticReportService reportService;

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    @Operation(summary = "Заключение по ID")
    public ApiResponse<DiagnosticReportResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(reportService.getById(id));
    }

    @GetMapping("/patient/{id}")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Заключение по ID (для пациента, проверка владельца)")
    public ApiResponse<DiagnosticReportResponse> getByIdForPatient(@PathVariable Long id,
                                                                   @CurrentUserId UUID patientId) {
        return ApiResponse.success(reportService.getByIdForPatient(id, patientId));
    }

    @GetMapping("/doctor/{id}")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "Заключение по ID (для врача-автора)")
    public ApiResponse<DiagnosticReportResponse> getByIdForDoctor(@PathVariable Long id,
                                                                  @CurrentUserId UUID doctorId) {
        return ApiResponse.success(reportService.getByIdForDoctor(id, doctorId));
    }

    @GetMapping("/patient")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Все заключения пациента (с фильтрами)")
    public ApiResponse<PageResponse<DiagnosticReportResponse>> getPatientReports(
            @CurrentUserId UUID patientId,
            @RequestParam(required = false) Severity severity,
            @RequestParam(required = false) DiseaseType diagnosis,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) String q,
            Pageable pageable) {
        return ApiResponse.success(reportService.getPatientReports(
                patientId, severity, diagnosis, from, to, q, pageable));
    }

    @GetMapping("/doctor")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "Все заключения врача (с фильтрами)")
    public ApiResponse<PageResponse<DiagnosticReportResponse>> getDoctorReports(
            @CurrentUserId UUID doctorId,
            @RequestParam(required = false) Severity severity,
            @RequestParam(required = false) DiseaseType diagnosis,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) String q,
            Pageable pageable) {
        return ApiResponse.success(reportService.getDoctorReports(
                doctorId, severity, diagnosis, from, to, q, pageable));
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Все заключения (admin, с фильтрами)")
    public ApiResponse<PageResponse<DiagnosticReportResponse>> getAllReports(
            @RequestParam(required = false) Severity severity,
            @RequestParam(required = false) DiseaseType diagnosis,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) String q,
            Pageable pageable) {
        return ApiResponse.success(reportService.getAllReports(
                severity, diagnosis, from, to, q, pageable));
    }

    @GetMapping("/by-patient/{patientId}")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    @Operation(summary = "Все заключения конкретного пациента (для доктора/админа)",
            description = "Любой врач/админ видит отчёты пациента, не только свои.")
    public ApiResponse<PageResponse<DiagnosticReportResponse>> getByPatientId(
            @PathVariable UUID patientId,
            @RequestParam(required = false) Severity severity,
            @RequestParam(required = false) DiseaseType diagnosis,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) String q,
            Pageable pageable) {
        return ApiResponse.success(reportService.getByPatientId(
                patientId, severity, diagnosis, from, to, q, pageable));
    }

    @PostMapping
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "Создать заключение",
            description = "Публикует ReportCreatedEvent в Kafka для appointment- и xray-сервисов")
    public ApiResponse<DiagnosticReportResponse> create(@CurrentUserId UUID doctorId,
                                                        @Valid @RequestBody DiagnosticReportRequest request) {
        return ApiResponse.success("Report created", reportService.create(doctorId, request));
    }

    @PostMapping("/{id}/send")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "Отправить заключение пациенту")
    public ApiResponse<DiagnosticReportResponse> send(@PathVariable Long id,
                                                      @CurrentUserId UUID doctorId) {
        return ApiResponse.success("Sent", reportService.sendToPatient(id, doctorId));
    }
}
