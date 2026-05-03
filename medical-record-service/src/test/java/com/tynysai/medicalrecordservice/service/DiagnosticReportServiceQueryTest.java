package com.tynysai.medicalrecordservice.service;

import com.tynysai.common.client.dto.UserDto;
import com.tynysai.common.dto.PageResponse;
import com.tynysai.medicalrecordservice.client.UserClient;
import com.tynysai.medicalrecordservice.dto.response.DiagnosticReportResponse;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiagnosticReportServiceQueryTest {
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

    private UUID patientId;
    private UUID doctorId;
    private UserDto patient;
    private UserDto doctor;

    @BeforeEach
    void setUp() {
        patientId = UUID.randomUUID();
        doctorId = UUID.randomUUID();
        patient = userDto(patientId, "Иван Иванов", "PATIENT");
        doctor = userDto(doctorId, "Dr. House", "DOCTOR");
    }

    @Test
    void getById_returnsResponse_whenReportExists() {
        DiagnosticReport report = sampleReport(1L);
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        when(userClient.tryGetById(patientId)).thenReturn(patient);
        when(userClient.tryGetById(doctorId)).thenReturn(doctor);
        when(userClient.getDoctorSpecialization(doctorId)).thenReturn("Cardiology");

        DiagnosticReportResponse response = service.getById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getPatientName()).isEqualTo("Иван Иванов");
        assertThat(response.getDoctorName()).isEqualTo("Dr. House");
        assertThat(response.getDoctorSpecialization()).isEqualTo("Cardiology");
    }

    @Test
    void getById_throwsNotFound_whenMissing() {
        when(reportRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getByIdForPatient_returnsResponse_whenFound() {
        DiagnosticReport report = sampleReport(2L);
        when(reportRepository.findByIdAndPatientId(2L, patientId)).thenReturn(Optional.of(report));
        when(userClient.tryGetById(patientId)).thenReturn(patient);
        when(userClient.tryGetById(doctorId)).thenReturn(doctor);

        DiagnosticReportResponse response = service.getByIdForPatient(2L, patientId);

        assertThat(response.getId()).isEqualTo(2L);
    }

    @Test
    void getByIdForPatient_throwsNotFound_whenMissing() {
        when(reportRepository.findByIdAndPatientId(404L, patientId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByIdForPatient(404L, patientId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getByIdForDoctor_returnsResponse_whenFound() {
        DiagnosticReport report = sampleReport(3L);
        when(reportRepository.findByIdAndDoctorId(3L, doctorId)).thenReturn(Optional.of(report));
        when(userClient.tryGetById(patientId)).thenReturn(patient);
        when(userClient.tryGetById(doctorId)).thenReturn(doctor);

        DiagnosticReportResponse response = service.getByIdForDoctor(3L, doctorId);

        assertThat(response.getId()).isEqualTo(3L);
    }

    @Test
    void getByIdForDoctor_throwsNotFound_whenMissing() {
        when(reportRepository.findByIdAndDoctorId(404L, doctorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByIdForDoctor(404L, doctorId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getById_handlesNullDoctor_gracefully() {
        DiagnosticReport report = sampleReport(4L);
        when(reportRepository.findById(4L)).thenReturn(Optional.of(report));
        when(userClient.tryGetById(patientId)).thenReturn(null);
        when(userClient.tryGetById(doctorId)).thenReturn(null);

        DiagnosticReportResponse response = service.getById(4L);

        assertThat(response.getDoctorName()).isNull();
        assertThat(response.getPatientName()).isNull();
        assertThat(response.getDoctorSpecialization()).isNull();
    }

    @Test
    void getPatientReports_appliesDefaultSort_andCallsRepoFindAll() {
        Page<DiagnosticReport> page = new PageImpl<>(List.of(sampleReport(10L)));
        when(reportRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(userClient.tryGetById(patientId)).thenReturn(patient);
        when(userClient.tryGetById(doctorId)).thenReturn(doctor);

        Pageable unsorted = PageRequest.of(0, 10);
        PageResponse<DiagnosticReportResponse> response = service.getPatientReports(
                patientId, Severity.MILD, DiseaseType.NORMAL,
                LocalDateTime.now().minusDays(7), LocalDateTime.now(), "search", unsorted);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(reportRepository).findAll(any(Specification.class), captor.capture());
        assertThat(captor.getValue().getSort().getOrderFor("createdAt")).isNotNull();
        assertThat(response.getContent()).hasSize(1);
    }

    @Test
    void getPatientReports_keepsCallerSort_whenPageableSorted() {
        Page<DiagnosticReport> page = new PageImpl<>(List.of());
        when(reportRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Pageable sorted = PageRequest.of(0, 5, Sort.by(Sort.Direction.ASC, "reportNumber"));
        service.getPatientReports(patientId, null, null, null, null, null, sorted);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(reportRepository).findAll(any(Specification.class), captor.capture());
        assertThat(captor.getValue().getSort().getOrderFor("reportNumber")).isNotNull();
    }

    @Test
    void getDoctorReports_callsRepo_andUsesPatientNameSearch() {
        Page<DiagnosticReport> page = new PageImpl<>(List.of(sampleReport(20L)));
        when(reportRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(userClient.searchPatientIds("john")).thenReturn(List.of(patientId));
        when(userClient.tryGetById(patientId)).thenReturn(patient);
        when(userClient.tryGetById(doctorId)).thenReturn(doctor);

        PageResponse<DiagnosticReportResponse> response = service.getDoctorReports(
                doctorId, null, null, null, null, "john", PageRequest.of(0, 10));

        verify(reportRepository).findAll(any(Specification.class), any(Pageable.class));
        verify(userClient).searchPatientIds("john");
        assertThat(response.getContent()).hasSize(1);
    }

    @Test
    void getDoctorReports_skipsPatientSearch_whenQueryBlank() {
        Page<DiagnosticReport> page = new PageImpl<>(List.of());
        when(reportRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        service.getDoctorReports(doctorId, null, null, null, null, "  ", PageRequest.of(0, 10));

        verify(reportRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void getByPatientId_callsRepoFindAll() {
        Page<DiagnosticReport> page = new PageImpl<>(List.of(sampleReport(30L)));
        when(reportRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(userClient.tryGetById(patientId)).thenReturn(patient);
        when(userClient.tryGetById(doctorId)).thenReturn(doctor);

        PageResponse<DiagnosticReportResponse> response = service.getByPatientId(
                patientId, Severity.MODERATE, DiseaseType.NORMAL,
                LocalDateTime.now().minusDays(30), LocalDateTime.now(), "x", PageRequest.of(0, 10));

        assertThat(response.getContent()).hasSize(1);
        verify(reportRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void getAllReports_callsRepoFindAll() {
        Page<DiagnosticReport> page = new PageImpl<>(List.of(sampleReport(40L)));
        when(reportRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(userClient.searchPatientIds("foo")).thenReturn(List.of(patientId));
        when(userClient.tryGetById(patientId)).thenReturn(patient);
        when(userClient.tryGetById(doctorId)).thenReturn(doctor);

        PageResponse<DiagnosticReportResponse> response = service.getAllReports(
                Severity.SEVERE, DiseaseType.NORMAL, null, null, "foo", PageRequest.of(0, 10));

        assertThat(response.getContent()).hasSize(1);
        verify(reportRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void getAllReports_skipsPatientSearch_whenQueryNull() {
        Page<DiagnosticReport> page = new PageImpl<>(List.of());
        when(reportRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        service.getAllReports(null, null, null, null, null, PageRequest.of(0, 10));

        verify(reportRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    private DiagnosticReport sampleReport(Long id) {
        return DiagnosticReport.builder()
                .id(id)
                .reportNumber("RPT-" + id)
                .patientId(patientId)
                .doctorId(doctorId)
                .finalDiagnosis(DiseaseType.NORMAL)
                .severity(Severity.NONE)
                .clinicalFindings("ok")
                .reportText("text")
                .build();
    }

    private static UserDto userDto(UUID id, String fullName, String role) {
        UserDto u = new UserDto();
        u.setId(id);
        u.setFullName(fullName);
        u.setRole(role);
        return u;
    }
}
