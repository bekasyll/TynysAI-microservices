package com.tynysai.xrayservice.controller;

import com.tynysai.xrayservice.dto.ApiResponse;
import com.tynysai.xrayservice.dto.PageResponse;
import com.tynysai.xrayservice.dto.request.DoctorValidationRequest;
import com.tynysai.xrayservice.dto.response.XrayAnalysisResponse;
import com.tynysai.xrayservice.service.XrayAnalysisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/xrays")
@RequiredArgsConstructor
public class XrayAnalysisController {
    private final XrayAnalysisService xrayAnalysisService;

    @GetMapping("/{id}")
    public ApiResponse<XrayAnalysisResponse> getById(@PathVariable Long id,
                                                     @RequestParam(required = false) Long patientId) {
        if (patientId != null) {
            return ApiResponse.success(xrayAnalysisService.getByIdForPatient(id, patientId));
        }
        return ApiResponse.success(xrayAnalysisService.getById(id));
    }

    @GetMapping("/patient")
    public ApiResponse<PageResponse<XrayAnalysisResponse>> getPatientAnalyses(
            @RequestHeader("X-User-Id") Long patientId,
            Pageable pageable) {
        return ApiResponse.success(xrayAnalysisService.getPatientAnalyses(patientId, pageable));
    }

    @GetMapping("/doctor/assigned")
    public ApiResponse<PageResponse<XrayAnalysisResponse>> getDoctorAssigned(
            @RequestHeader("X-User-Id") Long doctorId,
            Pageable pageable) {
        return ApiResponse.success(xrayAnalysisService.getAssignedToDoctor(doctorId, pageable));
    }

    @GetMapping("/admin/all")
    public ApiResponse<PageResponse<XrayAnalysisResponse>> getAllAnalyses(Pageable pageable) {
        return ApiResponse.success(xrayAnalysisService.getAllAnalyses(pageable));
    }

    @GetMapping("/doctor/{id}")
    public ApiResponse<XrayAnalysisResponse> getByIdForDoctor(@PathVariable Long id,
                                                              @RequestHeader("X-User-Id") Long doctorId) {
        return ApiResponse.success(xrayAnalysisService.getByIdForDoctor(id, doctorId));
    }

    @PostMapping(value = "/patient/upload", consumes = "multipart/form-data")
    public ApiResponse<XrayAnalysisResponse> uploadByPatient(
            @RequestHeader("X-User-Id") Long patientId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String patientNotes,
            @RequestParam(required = false) Long assignedDoctorId) {
        return ApiResponse.success("Uploaded",
                xrayAnalysisService.uploadAndAnalyze(patientId, file, patientNotes, assignedDoctorId));
    }

    @PostMapping(value = "/doctor/upload", consumes = "multipart/form-data")
    public ApiResponse<XrayAnalysisResponse> uploadByDoctor(
            @RequestHeader("X-User-Id") Long doctorId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String notes) {
        return ApiResponse.success("Uploaded",
                xrayAnalysisService.uploadAndAnalyzeByDoctor(doctorId, file, notes));
    }

    @PostMapping("/{id}/validate")
    public ApiResponse<XrayAnalysisResponse> validate(@PathVariable Long id,
                                                      @RequestHeader("X-User-Id") Long doctorId,
                                                      @Valid @RequestBody DoctorValidationRequest request) {
        return ApiResponse.success("Validated", xrayAnalysisService.validate(id, doctorId, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, @RequestHeader("X-User-Id") Long patientId) {
        xrayAnalysisService.delete(id, patientId);
        return ApiResponse.success("Deleted", null);
    }
}
