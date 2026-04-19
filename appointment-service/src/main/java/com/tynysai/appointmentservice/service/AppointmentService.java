package com.tynysai.appointmentservice.service;

import com.tynysai.appointmentservice.client.UserClient;
import com.tynysai.appointmentservice.client.XrayClient;
import com.tynysai.appointmentservice.client.dto.UserDto;
import com.tynysai.appointmentservice.dto.PageResponse;
import com.tynysai.appointmentservice.dto.request.AppointmentDecisionRequest;
import com.tynysai.appointmentservice.dto.request.AppointmentRequest;
import com.tynysai.appointmentservice.dto.response.AppointmentResponse;
import com.tynysai.appointmentservice.exception.BadRequestException;
import com.tynysai.appointmentservice.exception.ResourceNotFoundException;
import com.tynysai.appointmentservice.kafka.NotificationEventPublisher;
import com.tynysai.appointmentservice.model.Appointment;
import com.tynysai.appointmentservice.model.enums.AppointmentStatus;
import com.tynysai.appointmentservice.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppointmentService {
    private final AppointmentRepository appointmentRepository;
    private final UserClient userClient;
    private final XrayClient xrayClient;
    private final NotificationEventPublisher notificationPublisher;

    public PageResponse<AppointmentResponse> getPatientAppointments(Long patientId,
                                                                    AppointmentStatus status,
                                                                    Pageable pageable) {
        Page<Appointment> page = status != null
                ? appointmentRepository.findByPatientIdAndStatusOrderByCreatedAtDesc(patientId, status, pageable)
                : appointmentRepository.findByPatientIdOrderByCreatedAtDesc(patientId, pageable);
        return PageResponse.from(page.map(this::toResponse));
    }

    public PageResponse<AppointmentResponse> getDoctorAppointments(Long doctorId,
                                                                   AppointmentStatus status,
                                                                   Pageable pageable) {
        Page<Appointment> page = status != null
                ? appointmentRepository.findByDoctorIdAndStatusOrderByCreatedAtDesc(doctorId, status, pageable)
                : appointmentRepository.findByDoctorIdOrderByCreatedAtDesc(doctorId, pageable);
        return PageResponse.from(page.map(this::toResponse));
    }

    public AppointmentResponse getByIdForPatient(Long appointmentId, Long patientId) {
        return toResponse(appointmentRepository.findByIdAndPatientId(appointmentId, patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", "id", appointmentId)));
    }

    public AppointmentResponse getByIdForDoctor(Long appointmentId, Long doctorId) {
        return toResponse(appointmentRepository.findByIdAndDoctorId(appointmentId, doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", "id", appointmentId)));
    }

    @Transactional
    public AppointmentResponse book(Long patientId, AppointmentRequest request) {
        UserDto patient = userClient.getById(patientId);
        UserDto doctor = userClient.getById(request.getDoctorId());

        if (!"DOCTOR".equalsIgnoreCase(doctor.getRole())) {
            throw new BadRequestException("Target user is not a doctor");
        }

        if (request.getAppointmentDate() != null) {
            LocalDateTime from = request.getAppointmentDate().minusMinutes(29);
            LocalDateTime to = request.getAppointmentDate().plusMinutes(29);
            if (appointmentRepository.existsConflict(doctor.getId(), from, to)) {
                throw new BadRequestException("This time slot is already taken. Please choose another time.");
            }
        }

        if (request.getXrayAnalysisId() != null && !xrayClient.existsForPatient(request.getXrayAnalysisId(), patientId)) {
            throw new ResourceNotFoundException("XrayAnalysis", "id", request.getXrayAnalysisId());
        }

        Appointment appointment = Appointment.builder()
                .patientId(patient.getId())
                .doctorId(doctor.getId())
                .appointmentDate(request.getAppointmentDate())
                .patientComplaints(request.getPatientComplaints())
                .xrayAnalysisId(request.getXrayAnalysisId())
                .status(AppointmentStatus.PENDING)
                .build();

        Appointment saved = appointmentRepository.save(appointment);

        notificationPublisher.publish(doctor.getId(),
                "APPOINTMENT_REQUESTED",
                saved.getId().toString(),
                "Appointment",
                Map.of("patientName", patient.getFullName()));

        return toResponse(saved, patient, doctor);
    }

    @Transactional
    public AppointmentResponse accept(Long appointmentId, Long doctorId, AppointmentDecisionRequest request) {
        Appointment appointment = appointmentRepository.findByIdAndDoctorId(appointmentId, doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", "id", appointmentId));

        if (appointment.getStatus() != AppointmentStatus.PENDING) {
            throw new BadRequestException("Only PENDING appointments can be accepted");
        }

        appointment.setStatus(AppointmentStatus.ACCEPTED);
        if (request != null) {
            if (request.getDoctorNotes() != null) appointment.setDoctorNotes(request.getDoctorNotes());
            if (request.getAppointmentDate() != null) appointment.setAppointmentDate(request.getAppointmentDate());
        }

        LocalDateTime finalDate = appointment.getAppointmentDate();
        if (finalDate != null) {
            LocalDateTime from = finalDate.minusMinutes(29);
            LocalDateTime to = finalDate.plusMinutes(29);
            if (appointmentRepository.existsAcceptedConflict(doctorId, appointmentId, from, to)) {
                throw new BadRequestException("You already have an accepted appointment at this time slot.");
            }
        }

        Appointment saved = appointmentRepository.save(appointment);

        UserDto doctor = userClient.getById(doctorId);
        notificationPublisher.publish(appointment.getPatientId(),
                "APPOINTMENT_ACCEPTED",
                saved.getId().toString(),
                "Appointment",
                Map.of("doctorName", doctor.getFullName()));

        return toResponse(saved);
    }

    @Transactional
    public AppointmentResponse reject(Long appointmentId, Long doctorId, AppointmentDecisionRequest request) {
        Appointment appointment = appointmentRepository.findByIdAndDoctorId(appointmentId, doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", "id", appointmentId));

        if (appointment.getStatus() != AppointmentStatus.PENDING) {
            throw new BadRequestException("Only PENDING appointments can be rejected");
        }

        appointment.setStatus(AppointmentStatus.REJECTED);
        if (request != null && request.getDoctorNotes() != null) {
            appointment.setDoctorNotes(request.getDoctorNotes());
        }

        Appointment saved = appointmentRepository.save(appointment);

        UserDto doctor = userClient.getById(doctorId);
        Map<String, String> params = new HashMap<>();
        params.put("doctorName", doctor.getFullName());
        if (request != null && request.getDoctorNotes() != null) {
            params.put("reason", request.getDoctorNotes());
        }
        notificationPublisher.publish(appointment.getPatientId(),
                "APPOINTMENT_REJECTED",
                saved.getId().toString(),
                "Appointment",
                params);

        return toResponse(saved);
    }

    @Transactional
    public AppointmentResponse cancel(Long appointmentId, Long patientId) {
        Appointment appointment = appointmentRepository.findByIdAndPatientId(appointmentId, patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", "id", appointmentId));

        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new BadRequestException("Completed appointments cannot be cancelled");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        Appointment saved = appointmentRepository.save(appointment);

        UserDto patient = userClient.getById(patientId);
        notificationPublisher.publish(appointment.getDoctorId(),
                "APPOINTMENT_CANCELLED",
                saved.getId().toString(),
                "Appointment",
                Map.of("patientName", patient.getFullName()));

        return toResponse(saved);
    }

    @Transactional
    public AppointmentResponse complete(Long appointmentId, Long doctorId, Long reportId) {
        Appointment appointment = appointmentRepository.findByIdAndDoctorId(appointmentId, doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", "id", appointmentId));

        if (appointment.getStatus() != AppointmentStatus.ACCEPTED) {
            throw new BadRequestException("Only ACCEPTED appointments can be completed");
        }

        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointment.setReportId(reportId);
        Appointment saved = appointmentRepository.save(appointment);

        UserDto doctor = userClient.getById(doctorId);
        notificationPublisher.publish(appointment.getPatientId(),
                "APPOINTMENT_COMPLETED",
                saved.getId().toString(),
                "Appointment",
                Map.of("doctorName", doctor.getFullName()));

        return toResponse(saved);
    }

    // Called from the Kafka report-events listener after a diagnostic report is created
    @Transactional
    public void linkReport(Long appointmentId, Long doctorId, Long reportId) {
        appointmentRepository.findByIdAndDoctorId(appointmentId, doctorId)
                .ifPresent(appointment -> {
                    appointment.setReportId(reportId);
                    if (appointment.getStatus() == AppointmentStatus.ACCEPTED) {
                        appointment.setStatus(AppointmentStatus.COMPLETED);
                    }
                    appointmentRepository.save(appointment);
                });
    }

    private AppointmentResponse toResponse(Appointment a) {
        UserDto patient = userClient.tryFetchUser(a.getPatientId());
        UserDto doctor = userClient.tryFetchUser(a.getDoctorId());
        return toResponse(a, patient, doctor);
    }

    private AppointmentResponse toResponse(Appointment a, UserDto patient, UserDto doctor) {
        String doctorSpecialization = null;
        if (doctor != null) {
            doctorSpecialization = userClient.getDoctorSpecialization(doctor.getId());
        }
        return AppointmentResponse.builder()
                .id(a.getId())
                .patientId(a.getPatientId())
                .patientName(patient != null ? patient.getFullName() : null)
                .doctorId(a.getDoctorId())
                .doctorName(doctor != null ? doctor.getFullName() : null)
                .doctorSpecialization(doctorSpecialization)
                .status(a.getStatus())
                .appointmentDate(a.getAppointmentDate())
                .patientComplaints(a.getPatientComplaints())
                .doctorNotes(a.getDoctorNotes())
                .reportId(a.getReportId())
                .xrayAnalysisId(a.getXrayAnalysisId())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }
}