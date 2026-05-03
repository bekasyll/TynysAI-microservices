package com.tynysai.medicalrecordservice.service;

import com.tynysai.common.client.dto.UserDto;
import com.tynysai.medicalrecordservice.client.UserClient;
import com.tynysai.medicalrecordservice.dto.request.DiagnosticReportRequest;
import com.tynysai.medicalrecordservice.events.ReportCreatedEvent;
import com.tynysai.medicalrecordservice.exception.BadRequestException;
import com.tynysai.medicalrecordservice.exception.ResourceNotFoundException;
import com.tynysai.medicalrecordservice.kafka.NotificationEventPublisher;
import com.tynysai.medicalrecordservice.kafka.ReportEventPublisher;
import com.tynysai.medicalrecordservice.model.DiagnosticReport;
import com.tynysai.medicalrecordservice.model.enums.DiseaseType;
import com.tynysai.medicalrecordservice.model.enums.Severity;
import com.tynysai.medicalrecordservice.repository.DiagnosticReportRepository;
import com.tynysai.medicalrecordservice.repository.LabResultRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiagnosticReportServiceTest {
    @Mock
    private DiagnosticReportRepository reportRepository;
    @Mock
    private LabResultRepository labResultRepository;
    @Mock
    private UserClient userClient;
    @Mock
    private NotificationEventPublisher notificationPublisher;
    @Mock
    private ReportEventPublisher reportEventPublisher;

    @InjectMocks
    private DiagnosticReportService service;

    private UUID doctorId;
    private UUID patientId;
    private UserDto doctor;
    private UserDto patient;

    @BeforeEach
    void setUp() {
        doctorId = UUID.randomUUID();
        patientId = UUID.randomUUID();
        doctor = userDto(doctorId, "Dr. House", "DOCTOR");
        patient = userDto(patientId, "Иван Иванов", "PATIENT");
    }

    @Test
    void create_throwsBadRequest_whenTargetUserIsNotPatient() {
        when(userClient.getById(doctorId)).thenReturn(doctor);
        when(userClient.getById(patientId)).thenReturn(userDto(patientId, "x", "DOCTOR"));

        assertThatThrownBy(() -> service.create(doctorId, request()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not a patient");
        verify(reportRepository, never()).save(any());
    }

    @Test
    void create_throwsNotFound_whenLabResultIdSetButMissing() {
        when(userClient.getById(doctorId)).thenReturn(doctor);
        when(userClient.getById(patientId)).thenReturn(patient);
        when(labResultRepository.findById(99L)).thenReturn(Optional.empty());

        DiagnosticReportRequest req = request();
        req.setLabResultId(99L);

        assertThatThrownBy(() -> service.create(doctorId, req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_savesReport_publishesReportEvent_andNotifies_whenSendToPatientTrue() {
        when(userClient.getById(doctorId)).thenReturn(doctor);
        when(userClient.getById(patientId)).thenReturn(patient);
        when(reportRepository.save(any(DiagnosticReport.class))).thenAnswer(inv -> {
            DiagnosticReport r = inv.getArgument(0);
            r.setId(42L);
            return r;
        });

        service.create(doctorId, request());

        ArgumentCaptor<DiagnosticReport> saved = ArgumentCaptor.forClass(DiagnosticReport.class);
        verify(reportRepository).save(saved.capture());
        assertThat(saved.getValue().getReportNumber()).startsWith("RPT-");
        assertThat(saved.getValue().isSentToPatient()).isTrue();
        assertThat(saved.getValue().getSentAt()).isNotNull();

        ArgumentCaptor<ReportCreatedEvent> event = ArgumentCaptor.forClass(ReportCreatedEvent.class);
        verify(reportEventPublisher).publishReportCreated(event.capture());
        assertThat(event.getValue().getReportId()).isEqualTo(42L);
        assertThat(event.getValue().getPatientId()).isEqualTo(patientId);

        verify(notificationPublisher).publish(eq(patientId), eq("REPORT_READY"), eq("42"), any(), any());
    }

    @Test
    void create_savesAsDraft_andSkipsNotification_whenSendToPatientFalse() {
        when(userClient.getById(doctorId)).thenReturn(doctor);
        when(userClient.getById(patientId)).thenReturn(patient);
        when(reportRepository.save(any(DiagnosticReport.class))).thenAnswer(inv -> {
            DiagnosticReport r = inv.getArgument(0);
            r.setId(42L);
            return r;
        });

        DiagnosticReportRequest req = request();
        req.setSendToPatient(false);

        service.create(doctorId, req);

        ArgumentCaptor<DiagnosticReport> captor = ArgumentCaptor.forClass(DiagnosticReport.class);
        verify(reportRepository).save(captor.capture());
        assertThat(captor.getValue().isSentToPatient()).isFalse();
        assertThat(captor.getValue().getSentAt()).isNull();

        verify(reportEventPublisher).publishReportCreated(any());
        verify(notificationPublisher, never()).publish(any(), any(), any(), any(), any());
    }

    @Test
    void sendToPatient_throwsNotFound_whenReportMissing() {
        when(reportRepository.findByIdAndDoctorId(1L, doctorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.sendToPatient(1L, doctorId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void sendToPatient_marksSent_andNotifiesPatient() {
        DiagnosticReport report = DiagnosticReport.builder()
                .id(1L)
                .patientId(patientId)
                .doctorId(doctorId)
                .finalDiagnosis(DiseaseType.NORMAL)
                .severity(Severity.NONE)
                .sentToPatient(false)
                .build();
        when(reportRepository.findByIdAndDoctorId(1L, doctorId)).thenReturn(Optional.of(report));
        when(reportRepository.save(any(DiagnosticReport.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userClient.tryGetById(doctorId)).thenReturn(doctor);
        when(userClient.tryGetById(patientId)).thenReturn(patient);

        service.sendToPatient(1L, doctorId);

        assertThat(report.isSentToPatient()).isTrue();
        assertThat(report.getSentAt()).isNotNull();
        verify(notificationPublisher).publish(eq(patientId), eq("REPORT_READY"), eq("1"), any(), any());
    }

    @Test
    void sendToPatient_handlesUnknownDoctorName_gracefully() {
        DiagnosticReport report = DiagnosticReport.builder()
                .id(1L)
                .patientId(patientId)
                .doctorId(doctorId)
                .finalDiagnosis(DiseaseType.NORMAL)
                .severity(Severity.NONE)
                .build();
        when(reportRepository.findByIdAndDoctorId(1L, doctorId)).thenReturn(Optional.of(report));
        when(reportRepository.save(any(DiagnosticReport.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userClient.tryGetById(doctorId)).thenReturn(null);
        when(userClient.tryGetById(patientId)).thenReturn(null);

        service.sendToPatient(1L, doctorId);

        verify(notificationPublisher).publish(eq(patientId), eq("REPORT_READY"), any(), any(), any());
    }

    private DiagnosticReportRequest request() {
        DiagnosticReportRequest r = new DiagnosticReportRequest();
        r.setPatientId(patientId);
        r.setFinalDiagnosis(DiseaseType.NORMAL);
        r.setSeverity(Severity.NONE);
        r.setClinicalFindings("clear");
        r.setReportText("all good");
        r.setSendToPatient(true);
        return r;
    }

    private static UserDto userDto(UUID id, String fullName, String role) {
        UserDto u = new UserDto();
        u.setId(id);
        u.setFullName(fullName);
        u.setRole(role);
        return u;
    }
}