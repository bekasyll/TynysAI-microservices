package com.tynysai.medicalrecordservice.service;

import com.tynysai.common.client.dto.UserDto;
import com.tynysai.common.dto.PageResponse;
import com.tynysai.medicalrecordservice.client.UserClient;
import com.tynysai.medicalrecordservice.dto.request.LabResultRequest;
import com.tynysai.medicalrecordservice.dto.response.LabResultResponse;
import com.tynysai.medicalrecordservice.exception.BadRequestException;
import com.tynysai.medicalrecordservice.exception.ResourceNotFoundException;
import com.tynysai.medicalrecordservice.kafka.NotificationEventPublisher;
import com.tynysai.medicalrecordservice.model.LabResult;
import com.tynysai.medicalrecordservice.model.enums.LabTestType;
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

import java.time.LocalDate;
import java.util.List;
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
class LabResultServiceTest {
    @Mock
    private LabResultRepository labResultRepository;
    @Mock
    private UserClient userClient;
    @Mock
    private NotificationEventPublisher notificationEventPublisher;

    @InjectMocks
    private LabResultService service;

    private UUID doctorId;
    private UUID patientId;
    private UserDto doctor;
    private UserDto patient;

    @BeforeEach
    void setUp() {
        doctorId = UUID.randomUUID();
        patientId = UUID.randomUUID();
        doctor = userDto(doctorId, "Dr. Strange", "DOCTOR");
        patient = userDto(patientId, "Patient One", "PATIENT");
    }

    @Test
    void create_throwsBadRequest_whenTargetUserIsNotPatient() {
        when(userClient.getById(doctorId)).thenReturn(doctor);
        when(userClient.getById(patientId)).thenReturn(userDto(patientId, "x", "DOCTOR"));

        assertThatThrownBy(() -> service.create(doctorId, request()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not a patient");
        verify(labResultRepository, never()).save(any());
    }

    @Test
    void create_savesLabResult_andNotifiesPatient_whenValid() {
        when(userClient.getById(doctorId)).thenReturn(doctor);
        when(userClient.getById(patientId)).thenReturn(patient);
        when(labResultRepository.save(any(LabResult.class))).thenAnswer(inv -> {
            LabResult r = inv.getArgument(0);
            r.setId(7L);
            return r;
        });

        LabResultResponse response = service.create(doctorId, request());

        ArgumentCaptor<LabResult> saved = ArgumentCaptor.forClass(LabResult.class);
        verify(labResultRepository).save(saved.capture());
        assertThat(saved.getValue().getPatientId()).isEqualTo(patientId);
        assertThat(saved.getValue().getAddedByDoctorId()).isEqualTo(doctorId);
        assertThat(saved.getValue().getTestType()).isEqualTo(LabTestType.COMPLETE_BLOOD_COUNT);

        verify(notificationEventPublisher).publish(eq(patientId), eq("LAB_RESULT_ADDED"),
                eq("7"), eq("LabResult"), any());

        assertThat(response.getId()).isEqualTo(7L);
        assertThat(response.getPatientName()).isEqualTo("Patient One");
        assertThat(response.getAddedByDoctorName()).isEqualTo("Dr. Strange");
    }

    @Test
    void getById_returnsResponse_whenLabResultExists() {
        LabResult lab = sampleLab(11L);
        when(labResultRepository.findById(11L)).thenReturn(Optional.of(lab));
        when(userClient.tryGetById(patientId)).thenReturn(patient);
        when(userClient.tryGetById(doctorId)).thenReturn(doctor);

        LabResultResponse response = service.getById(11L);

        assertThat(response.getId()).isEqualTo(11L);
        assertThat(response.getPatientName()).isEqualTo("Patient One");
        assertThat(response.getAddedByDoctorName()).isEqualTo("Dr. Strange");
    }

    @Test
    void getById_throwsNotFound_whenLabResultMissing() {
        when(labResultRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getById_returnsResponse_whenDoctorIdNull() {
        LabResult lab = sampleLab(12L);
        lab.setAddedByDoctorId(null);
        when(labResultRepository.findById(12L)).thenReturn(Optional.of(lab));
        when(userClient.tryGetById(patientId)).thenReturn(patient);

        LabResultResponse response = service.getById(12L);

        assertThat(response.getAddedByDoctorName()).isNull();
    }

    @Test
    void getByIdForPatient_returnsResponse_whenFound() {
        LabResult lab = sampleLab(20L);
        when(labResultRepository.findByIdAndPatientId(20L, patientId)).thenReturn(Optional.of(lab));
        when(userClient.tryGetById(patientId)).thenReturn(patient);
        when(userClient.tryGetById(doctorId)).thenReturn(doctor);

        LabResultResponse response = service.getByIdForPatient(20L, patientId);

        assertThat(response.getId()).isEqualTo(20L);
    }

    @Test
    void getByIdForPatient_throwsNotFound_whenMissing() {
        when(labResultRepository.findByIdAndPatientId(404L, patientId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByIdForPatient(404L, patientId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getPatientLabResults_appliesDefaultSort_whenPageableUnsorted() {
        LabResult lab = sampleLab(30L);
        Page<LabResult> page = new PageImpl<>(List.of(lab));
        when(labResultRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(userClient.tryGetById(patientId)).thenReturn(patient);
        when(userClient.tryGetById(doctorId)).thenReturn(doctor);

        Pageable unsorted = PageRequest.of(0, 10);

        PageResponse<LabResultResponse> response = service.getPatientLabResults(
                patientId, LabTestType.COMPLETE_BLOOD_COUNT, LocalDate.now().minusDays(30),
                LocalDate.now(), "labName", unsorted);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(labResultRepository).findAll(any(Specification.class), captor.capture());
        assertThat(captor.getValue().getSort().isSorted()).isTrue();
        assertThat(captor.getValue().getSort().getOrderFor("testDate")).isNotNull();

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getId()).isEqualTo(30L);
    }

    @Test
    void getPatientLabResults_keepsExistingSort_whenPageableSorted() {
        Page<LabResult> page = new PageImpl<>(List.of());
        when(labResultRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Pageable sorted = PageRequest.of(0, 5, Sort.by(Sort.Direction.ASC, "labName"));

        service.getPatientLabResults(patientId, null, null, null, null, sorted);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(labResultRepository).findAll(any(Specification.class), captor.capture());
        assertThat(captor.getValue().getSort().getOrderFor("labName")).isNotNull();
    }

    @Test
    void delete_removesLabResult_whenFound() {
        LabResult lab = sampleLab(50L);
        when(labResultRepository.findById(50L)).thenReturn(Optional.of(lab));

        service.delete(50L);

        verify(labResultRepository).delete(lab);
    }

    @Test
    void delete_throwsNotFound_whenMissing() {
        when(labResultRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(404L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(labResultRepository, never()).delete(any(LabResult.class));
    }

    private LabResultRequest request() {
        LabResultRequest r = new LabResultRequest();
        r.setPatientId(patientId);
        r.setTestType(LabTestType.COMPLETE_BLOOD_COUNT);
        r.setTestDate(LocalDate.now());
        r.setLabName("Central Lab");
        r.setHemoglobin(13.5);
        return r;
    }

    private LabResult sampleLab(Long id) {
        return LabResult.builder()
                .id(id)
                .patientId(patientId)
                .addedByDoctorId(doctorId)
                .testType(LabTestType.COMPLETE_BLOOD_COUNT)
                .testDate(LocalDate.now())
                .labName("Central Lab")
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
