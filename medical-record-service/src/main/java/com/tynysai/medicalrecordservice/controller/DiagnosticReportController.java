package com.tynysai.medicalrecordservice.controller;

import com.tynysai.common.dto.ApiResponse;
import com.tynysai.common.dto.PageResponse;
import com.tynysai.medicalrecordservice.dto.request.DiagnosticReportRequest;
import com.tynysai.medicalrecordservice.dto.response.DiagnosticReportResponse;
import com.tynysai.common.security.CurrentUserId;
import com.tynysai.medicalrecordservice.service.DiagnosticReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
    @Operation(summary = "Все заключения пациента")
    public ApiResponse<PageResponse<DiagnosticReportResponse>> getPatientReports(
            @CurrentUserId UUID patientId,
            Pageable pageable) {
        return ApiResponse.success(reportService.getPatientReports(patientId, pageable));
    }

    @GetMapping("/doctor")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "Все заключения врача")
    public ApiResponse<PageResponse<DiagnosticReportResponse>> getDoctorReports(
            @CurrentUserId UUID doctorId,
            Pageable pageable) {
        return ApiResponse.success(reportService.getDoctorReports(doctorId, pageable));
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Все заключения (admin)")
    public ApiResponse<PageResponse<DiagnosticReportResponse>> getAllReports(Pageable pageable) {
        return ApiResponse.success(reportService.getAllReports(pageable));
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
