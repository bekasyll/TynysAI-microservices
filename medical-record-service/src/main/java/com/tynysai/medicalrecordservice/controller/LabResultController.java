package com.tynysai.medicalrecordservice.controller;

import com.tynysai.medicalrecordservice.dto.ApiResponse;
import com.tynysai.medicalrecordservice.dto.PageResponse;
import com.tynysai.medicalrecordservice.dto.request.LabResultRequest;
import com.tynysai.medicalrecordservice.dto.response.LabResultResponse;
import com.tynysai.medicalrecordservice.service.LabResultService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lab-results")
@RequiredArgsConstructor
public class LabResultController {
    private final LabResultService labResultService;

    @GetMapping("/{id}")
    public ApiResponse<LabResultResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(labResultService.getById(id));
    }

    @GetMapping("/patient/{id}")
    public ApiResponse<LabResultResponse> getByIdForPatient(@PathVariable Long id,
                                                            @RequestHeader("X-User-Id") Long patientId) {
        return ApiResponse.success(labResultService.getByIdForPatient(id, patientId));
    }

    @GetMapping("/patient")
    public ApiResponse<PageResponse<LabResultResponse>> getPatientLabResults(
            @RequestHeader("X-User-Id") Long patientId,
            Pageable pageable) {
        return ApiResponse.success(labResultService.getPatientLabResults(patientId, pageable));
    }

    @PostMapping
    public ApiResponse<LabResultResponse> create(@RequestHeader("X-User-Id") Long doctorId,
                                                 @Valid @RequestBody LabResultRequest request) {
        return ApiResponse.success("Lab result created", labResultService.create(doctorId, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        labResultService.delete(id);
        return ApiResponse.success("Deleted", null);
    }
}
