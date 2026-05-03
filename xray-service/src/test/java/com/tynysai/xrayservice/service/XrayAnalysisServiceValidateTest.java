package com.tynysai.xrayservice.service;

import com.tynysai.common.client.dto.UserDto;
import com.tynysai.xrayservice.client.UserClient;
import com.tynysai.xrayservice.dto.request.DoctorValidationRequest;
import com.tynysai.xrayservice.exception.BadRequestException;
import com.tynysai.xrayservice.exception.ResourceNotFoundException;
import com.tynysai.xrayservice.kafka.NotificationEventPublisher;
import com.tynysai.xrayservice.model.XrayAnalysis;
import com.tynysai.xrayservice.model.enums.AnalysisStatus;
import com.tynysai.xrayservice.model.enums.DiseaseType;
import com.tynysai.xrayservice.repository.XrayAnalysisRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class XrayAnalysisServiceValidateTest {
    @Mock
    private XrayAnalysisRepository repository;
    @Mock
    private UserClient userClient;
    @Mock
    private NotificationEventPublisher notificationPublisher;

    @InjectMocks
    private XrayAnalysisService service;

    private UUID assignedDoctorId;
    private UUID otherDoctorId;
    private UUID patientId;
    private DoctorValidationRequest request;

    @BeforeEach
    void setUp() {
        assignedDoctorId = UUID.randomUUID();
        otherDoctorId = UUID.randomUUID();
        patientId = UUID.randomUUID();
        request = new DoctorValidationRequest();
        request.setDoctorDiagnosis(DiseaseType.NORMAL);
        request.setDoctorNotes("looks fine");
    }

    @Test
    void validate_throwsNotFound_whenAnalysisMissing() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.validate(99L, assignedDoctorId, request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void validate_throwsBadRequest_whenCallerIsNotAssignedDoctor() {
        XrayAnalysis a = analysis(AnalysisStatus.COMPLETED, assignedDoctorId);
        when(repository.findById(1L)).thenReturn(Optional.of(a));

        assertThatThrownBy(() -> service.validate(1L, otherDoctorId, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("assigned doctor");

        verify(repository, never()).save(any());
    }

    @Test
    void validate_throwsBadRequest_whenAnalysisHasNoAssignedDoctor() {
        XrayAnalysis a = analysis(AnalysisStatus.COMPLETED, null);
        when(repository.findById(1L)).thenReturn(Optional.of(a));

        assertThatThrownBy(() -> service.validate(1L, assignedDoctorId, request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void validate_throwsBadRequest_whenStatusIsPending() {
        XrayAnalysis a = analysis(AnalysisStatus.PENDING, assignedDoctorId);
        when(repository.findById(1L)).thenReturn(Optional.of(a));

        assertThatThrownBy(() -> service.validate(1L, assignedDoctorId, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not yet ready");
    }

    @Test
    void validate_throwsBadRequest_whenStatusIsProcessing() {
        XrayAnalysis a = analysis(AnalysisStatus.PROCESSING, assignedDoctorId);
        when(repository.findById(1L)).thenReturn(Optional.of(a));

        assertThatThrownBy(() -> service.validate(1L, assignedDoctorId, request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void validate_marksAnalysisValidated_whenAllChecksPass() {
        XrayAnalysis a = analysis(AnalysisStatus.COMPLETED, assignedDoctorId);
        a.setPatientId(patientId);
        when(repository.findById(1L)).thenReturn(Optional.of(a));

        UserDto doctor = new UserDto();
        doctor.setId(assignedDoctorId);
        doctor.setFullName("Dr. House");
        when(userClient.getById(assignedDoctorId)).thenReturn(doctor);
        when(userClient.tryFetchUser(any())).thenReturn(null);  // toResponse fetches names

        service.validate(1L, assignedDoctorId, request);

        assertThat(a.getStatus()).isEqualTo(AnalysisStatus.VALIDATED);
        assertThat(a.getValidatedByDoctorId()).isEqualTo(assignedDoctorId);
        assertThat(a.getDoctorDiagnosis()).isEqualTo(DiseaseType.NORMAL);
        assertThat(a.getDoctorNotes()).isEqualTo("looks fine");
        assertThat(a.getValidatedAt()).isNotNull();
        verify(repository).save(a);
        verify(notificationPublisher).publish(eq(patientId), eq("ANALYSIS_VALIDATED"), any(), any(), any());
    }

    @Test
    void validate_skipsNotification_whenAnalysisHasNoPatient() {
        XrayAnalysis a = analysis(AnalysisStatus.COMPLETED, assignedDoctorId);
        a.setPatientId(null);
        when(repository.findById(1L)).thenReturn(Optional.of(a));

        UserDto doctor = new UserDto();
        doctor.setFullName("Dr. House");
        when(userClient.getById(assignedDoctorId)).thenReturn(doctor);
        when(userClient.tryFetchUser(any())).thenReturn(null);

        service.validate(1L, assignedDoctorId, request);

        verify(notificationPublisher, never()).publish(any(), any(), any(), any(), any());
    }

    private static XrayAnalysis analysis(AnalysisStatus status, UUID assignedDoctorId) {
        return XrayAnalysis.builder()
                .id(1L)
                .status(status)
                .assignedDoctorId(assignedDoctorId)
                .build();
    }

    private static <T> T eq(T value) {
        return org.mockito.ArgumentMatchers.eq(value);
    }
}