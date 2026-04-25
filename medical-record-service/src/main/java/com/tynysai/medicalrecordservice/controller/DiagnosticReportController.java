package com.tynysai.medicalrecordservice.controller;

import com.tynysai.medicalrecordservice.dto.ApiResponse;
import com.tynysai.medicalrecordservice.dto.PageResponse;
import com.tynysai.medicalrecordservice.dto.request.DiagnosticReportRequest;
import com.tynysai.medicalrecordservice.dto.response.DiagnosticReportResponse;
import com.tynysai.medicalrecordservice.service.DiagnosticReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Diagnostic Reports", description = "Диагностические заключения врачей")
public class DiagnosticReportController {
    private final DiagnosticReportService reportService;

    @GetMapping("/{id}")
    @Operation(summary = "Заключение по ID")
    public ApiResponse<DiagnosticReportResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(reportService.getById(id));
    }

    @GetMapping("/patient/{id}")
    @Operation(summary = "Заключение по ID (для пациента, проверка владельца)")
    public ApiResponse<DiagnosticReportResponse> getByIdForPatient(@PathVariable Long id,
                                                                   @RequestHeader("X-User-Id") UUID patientId) {
        return ApiResponse.success(reportService.getByIdForPatient(id, patientId));
    }

    @GetMapping("/doctor/{id}")
    @Operation(summary = "Заключение по ID (для врача-автора)")
    public ApiResponse<DiagnosticReportResponse> getByIdForDoctor(@PathVariable Long id,
                                                                  @RequestHeader("X-User-Id") UUID doctorId) {
        return ApiResponse.success(reportService.getByIdForDoctor(id, doctorId));
    }

    @GetMapping("/patient")
    @Operation(summary = "Все заключения пациента")
    public ApiResponse<PageResponse<DiagnosticReportResponse>> getPatientReports(
            @RequestHeader("X-User-Id") UUID patientId,
            Pageable pageable) {
        return ApiResponse.success(reportService.getPatientReports(patientId, pageable));
    }

    @GetMapping("/doctor")
    @Operation(summary = "Все заключения врача")
    public ApiResponse<PageResponse<DiagnosticReportResponse>> getDoctorReports(
            @RequestHeader("X-User-Id") UUID doctorId,
            Pageable pageable) {
        return ApiResponse.success(reportService.getDoctorReports(doctorId, pageable));
    }

    @GetMapping("/admin/all")
    @Operation(summary = "Все заключения (admin)")
    public ApiResponse<PageResponse<DiagnosticReportResponse>> getAllReports(Pageable pageable) {
        return ApiResponse.success(reportService.getAllReports(pageable));
    }

    @PostMapping
    @Operation(summary = "Создать заключение",
            description = "Публикует ReportCreatedEvent в Kafka для appointment- и xray-сервисов")
    public ApiResponse<DiagnosticReportResponse> create(@RequestHeader("X-User-Id") UUID doctorId,
                                                        @Valid @RequestBody DiagnosticReportRequest request) {
        return ApiResponse.success("Report created", reportService.create(doctorId, request));
    }

    @PostMapping("/{id}/send")
    @Operation(summary = "Отправить заключение пациенту")
    public ApiResponse<DiagnosticReportResponse> send(@PathVariable Long id,
                                                      @RequestHeader("X-User-Id") UUID doctorId) {
        return ApiResponse.success("Sent", reportService.sendToPatient(id, doctorId));
    }
}
