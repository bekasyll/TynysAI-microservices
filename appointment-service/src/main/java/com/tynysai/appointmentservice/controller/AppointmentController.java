package com.tynysai.appointmentservice.controller;

import com.tynysai.appointmentservice.dto.ApiResponse;
import com.tynysai.appointmentservice.dto.PageResponse;
import com.tynysai.appointmentservice.dto.request.AppointmentDecisionRequest;
import com.tynysai.appointmentservice.dto.request.AppointmentRequest;
import com.tynysai.appointmentservice.dto.response.AppointmentResponse;
import com.tynysai.appointmentservice.model.enums.AppointmentStatus;
import com.tynysai.appointmentservice.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {
    private final AppointmentService appointmentService;

    @GetMapping("/patient")
    public ApiResponse<PageResponse<AppointmentResponse>> getPatientAppointments(
            @RequestHeader("X-User-Id") Long patientId,
            @RequestParam(required = false) AppointmentStatus status,
            Pageable pageable) {
        return ApiResponse.success(appointmentService.getPatientAppointments(patientId, status, pageable));
    }

    @GetMapping("/doctor")
    public ApiResponse<PageResponse<AppointmentResponse>> getDoctorAppointments(
            @RequestHeader("X-User-Id") Long doctorId,
            @RequestParam(required = false) AppointmentStatus status,
            Pageable pageable) {
        return ApiResponse.success(appointmentService.getDoctorAppointments(doctorId, status, pageable));
    }

    @GetMapping("/patient/{id}")
    public ApiResponse<AppointmentResponse> getByIdForPatient(@PathVariable Long id,
                                                              @RequestHeader("X-User-Id") Long patientId) {
        return ApiResponse.success(appointmentService.getByIdForPatient(id, patientId));
    }

    @GetMapping("/doctor/{id}")
    public ApiResponse<AppointmentResponse> getByIdForDoctor(@PathVariable Long id,
                                                             @RequestHeader("X-User-Id") Long doctorId) {
        return ApiResponse.success(appointmentService.getByIdForDoctor(id, doctorId));
    }

    @PostMapping
    public ApiResponse<AppointmentResponse> book(@RequestHeader("X-User-Id") Long patientId,
                                                 @Valid @RequestBody AppointmentRequest request) {
        return ApiResponse.success("Appointment booked", appointmentService.book(patientId, request));
    }

    @PostMapping("/{id}/accept")
    public ApiResponse<AppointmentResponse> accept(@PathVariable Long id,
                                                   @RequestHeader("X-User-Id") Long doctorId,
                                                   @RequestBody(required = false) AppointmentDecisionRequest request) {
        return ApiResponse.success("Accepted", appointmentService.accept(id, doctorId, request));
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<AppointmentResponse> reject(@PathVariable Long id,
                                                   @RequestHeader("X-User-Id") Long doctorId,
                                                   @RequestBody(required = false) AppointmentDecisionRequest request) {
        return ApiResponse.success("Rejected", appointmentService.reject(id, doctorId, request));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<AppointmentResponse> cancel(@PathVariable Long id,
                                                   @RequestHeader("X-User-Id") Long patientId) {
        return ApiResponse.success("Cancelled", appointmentService.cancel(id, patientId));
    }

    @PostMapping("/{id}/complete")
    public ApiResponse<AppointmentResponse> complete(@PathVariable Long id,
                                                     @RequestHeader("X-User-Id") Long doctorId,
                                                     @RequestParam(required = false) Long reportId) {
        return ApiResponse.success("Completed", appointmentService.complete(id, doctorId, reportId));
    }
}
