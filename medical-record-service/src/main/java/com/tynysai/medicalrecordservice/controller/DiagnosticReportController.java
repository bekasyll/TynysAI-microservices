package com.tynysai.medicalrecordservice.controller;

import com.tynysai.medicalrecordservice.dto.ApiResponse;
import com.tynysai.medicalrecordservice.dto.PageResponse;
import com.tynysai.medicalrecordservice.dto.request.DiagnosticReportRequest;
import com.tynysai.medicalrecordservice.dto.response.DiagnosticReportResponse;
import com.tynysai.medicalrecordservice.service.DiagnosticReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class DiagnosticReportController {
    private final DiagnosticReportService reportService;

    @GetMapping("/{id}")
    public ApiResponse<DiagnosticReportResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(reportService.getById(id));
    }

    @GetMapping("/patient/{id}")
    public ApiResponse<DiagnosticReportResponse> getByIdForPatient(@PathVariable Long id,
                                                                   @RequestHeader("X-User-Id") UUID patientId) {
        return ApiResponse.success(reportService.getByIdForPatient(id, patientId));
    }

    @GetMapping("/doctor/{id}")
    public ApiResponse<DiagnosticReportResponse> getByIdForDoctor(@PathVariable Long id,
                                                                  @RequestHeader("X-User-Id") UUID doctorId) {
        return ApiResponse.success(reportService.getByIdForDoctor(id, doctorId));
    }

    @GetMapping("/patient")
    public ApiResponse<PageResponse<DiagnosticReportResponse>> getPatientReports(
            @RequestHeader("X-User-Id") UUID patientId,
            Pageable pageable) {
        return ApiResponse.success(reportService.getPatientReports(patientId, pageable));
    }

    @GetMapping("/doctor")
    public ApiResponse<PageResponse<DiagnosticReportResponse>> getDoctorReports(
            @RequestHeader("X-User-Id") UUID doctorId,
            Pageable pageable) {
        return ApiResponse.success(reportService.getDoctorReports(doctorId, pageable));
    }

    @GetMapping("/admin/all")
    public ApiResponse<PageResponse<DiagnosticReportResponse>> getAllReports(Pageable pageable) {
        return ApiResponse.success(reportService.getAllReports(pageable));
    }

    @PostMapping
    public ApiResponse<DiagnosticReportResponse> create(@RequestHeader("X-User-Id") UUID doctorId,
                                                        @Valid @RequestBody DiagnosticReportRequest request) {
        return ApiResponse.success("Report created", reportService.create(doctorId, request));
    }

    @PostMapping("/{id}/send")
    public ApiResponse<DiagnosticReportResponse> send(@PathVariable Long id,
                                                      @RequestHeader("X-User-Id") UUID doctorId) {
        return ApiResponse.success("Sent", reportService.sendToPatient(id, doctorId));
    }
}
