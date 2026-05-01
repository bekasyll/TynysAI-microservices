package com.tynysai.medicalrecordservice.service;

import com.tynysai.medicalrecordservice.client.UserClient;
import com.tynysai.common.client.dto.UserDto;
import com.tynysai.common.dto.PageResponse;
import com.tynysai.medicalrecordservice.dto.request.DiagnosticReportRequest;
import com.tynysai.medicalrecordservice.dto.response.DiagnosticReportResponse;
import com.tynysai.medicalrecordservice.events.ReportCreatedEvent;
import com.tynysai.medicalrecordservice.exception.BadRequestException;
import com.tynysai.medicalrecordservice.exception.ResourceNotFoundException;
import com.tynysai.medicalrecordservice.kafka.NotificationEventPublisher;
import com.tynysai.medicalrecordservice.kafka.ReportEventPublisher;
import com.tynysai.medicalrecordservice.model.DiagnosticReport;
import com.tynysai.medicalrecordservice.model.enums.DiseaseType;
import com.tynysai.medicalrecordservice.model.enums.Severity;
import com.tynysai.medicalrecordservice.repository.DiagnosticReportRepository;
import com.tynysai.medicalrecordservice.repository.DiagnosticReportSpecs;
import com.tynysai.medicalrecordservice.repository.LabResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiagnosticReportService {
    private final DiagnosticReportRepository diagnosticReportRepository;
    private final LabResultRepository labResultRepository;
    private final UserClient userClient;
    private final NotificationEventPublisher notificationEventPublisher;
    private final ReportEventPublisher reportEventPublisher;

    @Transactional
    public DiagnosticReportResponse create(UUID doctorId, DiagnosticReportRequest request) {
        UserDto doctor = userClient.getById(doctorId);
        UserDto patient = userClient.getById(request.getPatientId());

        if (!"PATIENT".equalsIgnoreCase(patient.getRole())) {
            throw new BadRequestException("Target user is not a patient");
        }

        if (request.getLabResultId() != null && labResultRepository.findById(request.getLabResultId()).isEmpty()) {
            throw new ResourceNotFoundException("LabResult", "id", request.getLabResultId());
        }

        String reportNumber = "RPT-" + System.currentTimeMillis();

        DiagnosticReport report = DiagnosticReport.builder()
                .patientId(patient.getId())
                .doctorId(doctor.getId())
                .xrayAnalysisId(request.getXrayAnalysisId())
                .labResultId(request.getLabResultId())
                .finalDiagnosis(request.getFinalDiagnosis())
                .severity(request.getSeverity())
                .clinicalFindings(request.getClinicalFindings())
                .treatmentRecommendations(request.getTreatmentRecommendations())
                .medicationRecommendations(request.getMedicationRecommendations())
                .lifestyleRecommendations(request.getLifestyleRecommendations())
                .followUpDate(request.getFollowUpDate())
                .reportText(request.getReportText())
                .reportNumber(reportNumber)
                .sentToPatient(request.isSendToPatient())
                .sentAt(request.isSendToPatient() ? LocalDateTime.now() : null)
                .build();

        DiagnosticReport saved = diagnosticReportRepository.save(report);

        reportEventPublisher.publishReportCreated(ReportCreatedEvent.builder()
                .reportId(saved.getId())
                .patientId(saved.getPatientId())
                .doctorId(saved.getDoctorId())
                .appointmentId(request.getAppointmentId())
                .xrayAnalysisId(saved.getXrayAnalysisId())
                .build());

        if (request.isSendToPatient()) {
            notificationEventPublisher.publish(patient.getId(),
                    "REPORT_READY",
                    saved.getId().toString(),
                    "DiagnosticReport",
                    Map.of("doctorName", doctor.getFullName()));
        }

        return toResponse(saved, patient, doctor);
    }

    public DiagnosticReportResponse getById(Long reportId) {
        return toResponse(diagnosticReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("DiagnosticReport", "id", reportId)));
    }

    public DiagnosticReportResponse getByIdForPatient(Long reportId, UUID patientId) {
        return toResponse(diagnosticReportRepository.findByIdAndPatientId(reportId, patientId)
                .orElseThrow(() -> new ResourceNotFoundException("DiagnosticReport", "id", reportId)));
    }

    public DiagnosticReportResponse getByIdForDoctor(Long reportId, UUID doctorId) {
        return toResponse(diagnosticReportRepository.findByIdAndDoctorId(reportId, doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("DiagnosticReport", "id", reportId)));
    }

    public PageResponse<DiagnosticReportResponse> getPatientReports(UUID patientId,
                                                                     Severity severity,
                                                                     DiseaseType diagnosis,
                                                                     LocalDateTime from, LocalDateTime to,
                                                                     String q, Pageable pageable) {
        return runFiltered(Specification.allOf(
                DiagnosticReportSpecs.byPatient(patientId),
                DiagnosticReportSpecs.bySeverity(severity),
                DiagnosticReportSpecs.byDiagnosis(diagnosis),
                DiagnosticReportSpecs.createdFrom(from),
                DiagnosticReportSpecs.createdTo(to),
                DiagnosticReportSpecs.matchesQuery(q)
        ), pageable);
    }

    public PageResponse<DiagnosticReportResponse> getDoctorReports(UUID doctorId,
                                                                    Severity severity,
                                                                    DiseaseType diagnosis,
                                                                    LocalDateTime from, LocalDateTime to,
                                                                    String q, Pageable pageable) {
        return runFiltered(Specification.allOf(
                DiagnosticReportSpecs.byDoctor(doctorId),
                DiagnosticReportSpecs.bySeverity(severity),
                DiagnosticReportSpecs.byDiagnosis(diagnosis),
                DiagnosticReportSpecs.createdFrom(from),
                DiagnosticReportSpecs.createdTo(to),
                patientNameOnly(q)
        ), pageable);
    }

    public PageResponse<DiagnosticReportResponse> getByPatientId(UUID patientId,
                                                                  Severity severity,
                                                                  DiseaseType diagnosis,
                                                                  LocalDateTime from, LocalDateTime to,
                                                                  String q, Pageable pageable) {
        return runFiltered(Specification.allOf(
                DiagnosticReportSpecs.byPatient(patientId),
                DiagnosticReportSpecs.bySeverity(severity),
                DiagnosticReportSpecs.byDiagnosis(diagnosis),
                DiagnosticReportSpecs.createdFrom(from),
                DiagnosticReportSpecs.createdTo(to),
                DiagnosticReportSpecs.matchesQuery(q)
        ), pageable);
    }

    public PageResponse<DiagnosticReportResponse> getAllReports(Severity severity,
                                                                 DiseaseType diagnosis,
                                                                 LocalDateTime from, LocalDateTime to,
                                                                 String q, Pageable pageable) {
        return runFiltered(Specification.allOf(
                DiagnosticReportSpecs.bySeverity(severity),
                DiagnosticReportSpecs.byDiagnosis(diagnosis),
                DiagnosticReportSpecs.createdFrom(from),
                DiagnosticReportSpecs.createdTo(to),
                patientNameOnly(q)
        ), pageable);
    }

    private PageResponse<DiagnosticReportResponse> runFiltered(Specification<DiagnosticReport> spec,
                                                                Pageable pageable) {
        Pageable p = pageable.getSort().isUnsorted()
                ? PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                        Sort.by(Sort.Direction.DESC, "createdAt"))
                : pageable;
        return PageResponse.from(diagnosticReportRepository.findAll(spec, p).map(this::toResponse));
    }

    private Specification<DiagnosticReport> patientNameOnly(String q) {
        if (q == null || q.isBlank()) return (root, qq, cb) -> cb.conjunction();
        return DiagnosticReportSpecs.patientIdIn(userClient.searchPatientIds(q));
    }

    @Transactional
    public DiagnosticReportResponse sendToPatient(Long reportId, UUID doctorId) {
        DiagnosticReport report = diagnosticReportRepository.findByIdAndDoctorId(reportId, doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("DiagnosticReport", "id", reportId));

        report.setSentToPatient(true);
        report.setSentAt(LocalDateTime.now());
        DiagnosticReport saved = diagnosticReportRepository.save(report);

        UserDto doctor = userClient.tryGetById(doctorId);
        notificationEventPublisher.publish(report.getPatientId(),
                "REPORT_READY",
                reportId.toString(),
                "DiagnosticReport",
                Map.of("doctorName", doctor != null ? doctor.getFullName() : ""));

        return toResponse(saved);
    }

    private DiagnosticReportResponse toResponse(DiagnosticReport report) {
        UserDto patient = userClient.tryGetById(report.getPatientId());
        UserDto doctor = userClient.tryGetById(report.getDoctorId());
        return toResponse(report, patient, doctor);
    }

    private DiagnosticReportResponse toResponse(DiagnosticReport report, UserDto patient, UserDto doctor) {
        String doctorSpec = doctor != null ? userClient.getDoctorSpecialization(doctor.getId()) : null;
        return DiagnosticReportResponse.builder()
                .id(report.getId())
                .reportNumber(report.getReportNumber())
                .patientId(report.getPatientId())
                .patientName(patient != null ? patient.getFullName() : null)
                .doctorId(report.getDoctorId())
                .doctorName(doctor != null ? doctor.getFullName() : null)
                .doctorSpecialization(doctorSpec)
                .xrayAnalysisId(report.getXrayAnalysisId())
                .labResultId(report.getLabResultId())
                .finalDiagnosis(report.getFinalDiagnosis())
                .finalDiagnosisDisplayName(report.getFinalDiagnosis().getDisplayName())
                .severity(report.getSeverity())
                .severityDisplayName(report.getSeverity().getDisplayName())
                .clinicalFindings(report.getClinicalFindings())
                .treatmentRecommendations(report.getTreatmentRecommendations())
                .medicationRecommendations(report.getMedicationRecommendations())
                .lifestyleRecommendations(report.getLifestyleRecommendations())
                .followUpDate(report.getFollowUpDate())
                .reportText(report.getReportText())
                .sentToPatient(report.isSentToPatient())
                .sentAt(report.getSentAt())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .build();
    }
}