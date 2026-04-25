package com.tynysai.appointmentservice.controller;

import com.tynysai.appointmentservice.dto.ApiResponse;
import com.tynysai.appointmentservice.dto.PageResponse;
import com.tynysai.appointmentservice.dto.request.AppointmentDecisionRequest;
import com.tynysai.appointmentservice.dto.request.AppointmentRequest;
import com.tynysai.appointmentservice.dto.response.AppointmentResponse;
import com.tynysai.appointmentservice.model.enums.AppointmentStatus;
import com.tynysai.appointmentservice.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
@Tag(name = "Appointments", description = "Жизненный цикл записи: PENDING → ACCEPTED/REJECTED → COMPLETED/CANCELLED")
public class AppointmentController {
    private final AppointmentService appointmentService;

    @GetMapping("/patient")
    @Operation(summary = "Записи текущего пациента", description = "С фильтром по статусу и пагинацией")
    public ApiResponse<PageResponse<AppointmentResponse>> getPatientAppointments(
            @RequestHeader("X-User-Id") UUID patientId,
            @RequestParam(required = false) AppointmentStatus status,
            Pageable pageable) {
        return ApiResponse.success(appointmentService.getPatientAppointments(patientId, status, pageable));
    }

    @GetMapping("/doctor")
    @Operation(summary = "Записи к текущему врачу")
    public ApiResponse<PageResponse<AppointmentResponse>> getDoctorAppointments(
            @RequestHeader("X-User-Id") UUID doctorId,
            @RequestParam(required = false) AppointmentStatus status,
            Pageable pageable) {
        return ApiResponse.success(appointmentService.getDoctorAppointments(doctorId, status, pageable));
    }

    @GetMapping("/patient/{id}")
    @Operation(summary = "Запись по ID (для пациента)")
    public ApiResponse<AppointmentResponse> getByIdForPatient(@PathVariable Long id,
                                                              @RequestHeader("X-User-Id") UUID patientId) {
        return ApiResponse.success(appointmentService.getByIdForPatient(id, patientId));
    }

    @GetMapping("/doctor/{id}")
    @Operation(summary = "Запись по ID (для врача)")
    public ApiResponse<AppointmentResponse> getByIdForDoctor(@PathVariable Long id,
                                                             @RequestHeader("X-User-Id") UUID doctorId) {
        return ApiResponse.success(appointmentService.getByIdForDoctor(id, doctorId));
    }

    @PostMapping
    @Operation(summary = "Записаться к врачу", description = "Создаёт запись в статусе PENDING")
    public ApiResponse<AppointmentResponse> book(@RequestHeader("X-User-Id") UUID patientId,
                                                 @Valid @RequestBody AppointmentRequest request) {
        return ApiResponse.success("Appointment booked", appointmentService.book(patientId, request));
    }

    @PostMapping("/{id}/accept")
    @Operation(summary = "Принять запись (врач)")
    public ApiResponse<AppointmentResponse> accept(@PathVariable Long id,
                                                   @RequestHeader("X-User-Id") UUID doctorId,
                                                   @RequestBody(required = false) AppointmentDecisionRequest request) {
        return ApiResponse.success("Accepted", appointmentService.accept(id, doctorId, request));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Отклонить запись (врач)")
    public ApiResponse<AppointmentResponse> reject(@PathVariable Long id,
                                                   @RequestHeader("X-User-Id") UUID doctorId,
                                                   @RequestBody(required = false) AppointmentDecisionRequest request) {
        return ApiResponse.success("Rejected", appointmentService.reject(id, doctorId, request));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Отменить запись (пациент)")
    public ApiResponse<AppointmentResponse> cancel(@PathVariable Long id,
                                                   @RequestHeader("X-User-Id") UUID patientId) {
        return ApiResponse.success("Cancelled", appointmentService.cancel(id, patientId));
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Завершить приём", description = "Привязывает диагностический отчёт")
    public ApiResponse<AppointmentResponse> complete(@PathVariable Long id,
                                                     @RequestHeader("X-User-Id") UUID doctorId,
                                                     @RequestParam(required = false) Long reportId) {
        return ApiResponse.success("Completed", appointmentService.complete(id, doctorId, reportId));
    }
}
