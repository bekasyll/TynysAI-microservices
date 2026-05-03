package com.tynysai.appointmentservice.service;

import com.tynysai.appointmentservice.client.UserClient;
import com.tynysai.appointmentservice.client.XrayClient;
import com.tynysai.appointmentservice.dto.response.AppointmentResponse;
import com.tynysai.appointmentservice.exception.ResourceNotFoundException;
import com.tynysai.appointmentservice.kafka.NotificationEventPublisher;
import com.tynysai.appointmentservice.model.Appointment;
import com.tynysai.appointmentservice.model.enums.AppointmentStatus;
import com.tynysai.appointmentservice.repository.AppointmentRepository;
import com.tynysai.common.client.dto.UserDto;
import com.tynysai.common.dto.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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
class AppointmentServiceQueriesTest {
    @Mock
    private AppointmentRepository repository;
    @Mock
    private UserClient userClient;
    @Mock
    private XrayClient xrayClient;
    @Mock
    private NotificationEventPublisher notificationPublisher;

    @InjectMocks
    private AppointmentService service;

    private UUID patientId;
    private UUID doctorId;

    @BeforeEach
    void setUp() {
        patientId = UUID.randomUUID();
        doctorId = UUID.randomUUID();
    }

    @Test
    void getPatientAppointments_callsStatusFilteredRepoMethod_whenStatusIsProvided() {
        Pageable pageable = PageRequest.of(0, 10);
        Appointment a = appointment(AppointmentStatus.PENDING);
        Page<Appointment> page = new PageImpl<>(List.of(a));
        when(repository.findByPatientIdAndStatusOrderByCreatedAtDesc(patientId, AppointmentStatus.PENDING, pageable))
                .thenReturn(page);
        when(userClient.tryFetchUser(any())).thenReturn(null);

        PageResponse<AppointmentResponse> result = service.getPatientAppointments(patientId, AppointmentStatus.PENDING, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(1L);
        verify(repository).findByPatientIdAndStatusOrderByCreatedAtDesc(patientId, AppointmentStatus.PENDING, pageable);
        verify(repository, never()).findByPatientIdOrderByCreatedAtDesc(any(), any());
    }

    @Test
    void getPatientAppointments_callsUnfilteredRepoMethod_whenStatusIsNull() {
        Pageable pageable = PageRequest.of(0, 5);
        Page<Appointment> page = new PageImpl<>(List.of());
        when(repository.findByPatientIdOrderByCreatedAtDesc(patientId, pageable)).thenReturn(page);

        PageResponse<AppointmentResponse> result = service.getPatientAppointments(patientId, null, pageable);

        assertThat(result.getContent()).isEmpty();
        verify(repository).findByPatientIdOrderByCreatedAtDesc(patientId, pageable);
        verify(repository, never()).findByPatientIdAndStatusOrderByCreatedAtDesc(any(), any(), any());
    }

    @Test
    void getDoctorAppointments_callsStatusFilteredRepoMethod_whenStatusIsProvided() {
        Pageable pageable = PageRequest.of(1, 20);
        Appointment a = appointment(AppointmentStatus.ACCEPTED);
        Page<Appointment> page = new PageImpl<>(List.of(a));
        when(repository.findByDoctorIdAndStatusOrderByCreatedAtDesc(doctorId, AppointmentStatus.ACCEPTED, pageable))
                .thenReturn(page);
        when(userClient.tryFetchUser(any())).thenReturn(null);

        PageResponse<AppointmentResponse> result = service.getDoctorAppointments(doctorId, AppointmentStatus.ACCEPTED, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(repository).findByDoctorIdAndStatusOrderByCreatedAtDesc(doctorId, AppointmentStatus.ACCEPTED, pageable);
        verify(repository, never()).findByDoctorIdOrderByCreatedAtDesc(any(), any());
    }

    @Test
    void getDoctorAppointments_callsUnfilteredRepoMethod_whenStatusIsNull() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Appointment> page = new PageImpl<>(List.of());
        when(repository.findByDoctorIdOrderByCreatedAtDesc(doctorId, pageable)).thenReturn(page);

        PageResponse<AppointmentResponse> result = service.getDoctorAppointments(doctorId, null, pageable);

        assertThat(result.getContent()).isEmpty();
        verify(repository).findByDoctorIdOrderByCreatedAtDesc(doctorId, pageable);
        verify(repository, never()).findByDoctorIdAndStatusOrderByCreatedAtDesc(any(), any(), any());
    }

    @Test
    void getByIdForPatient_returnsResponse_whenAppointmentExists() {
        Appointment a = appointment(AppointmentStatus.PENDING);
        UserDto patient = userDto(patientId, "Иван", "PATIENT");
        UserDto doctor = userDto(doctorId, "Dr. House", "DOCTOR");
        when(repository.findByIdAndPatientId(1L, patientId)).thenReturn(Optional.of(a));
        when(userClient.tryFetchUser(patientId)).thenReturn(patient);
        when(userClient.tryFetchUser(doctorId)).thenReturn(doctor);
        when(userClient.getDoctorSpecialization(doctorId)).thenReturn("Pulmonology");

        AppointmentResponse result = service.getByIdForPatient(1L, patientId);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getPatientName()).isEqualTo("Иван");
        assertThat(result.getDoctorName()).isEqualTo("Dr. House");
        assertThat(result.getDoctorSpecialization()).isEqualTo("Pulmonology");
    }

    @Test
    void getByIdForPatient_throwsNotFound_whenAppointmentMissing() {
        when(repository.findByIdAndPatientId(eq(99L), eq(patientId))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByIdForPatient(99L, patientId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void getByIdForDoctor_returnsResponse_whenAppointmentExists() {
        Appointment a = appointment(AppointmentStatus.ACCEPTED);
        when(repository.findByIdAndDoctorId(1L, doctorId)).thenReturn(Optional.of(a));
        when(userClient.tryFetchUser(any())).thenReturn(null);

        AppointmentResponse result = service.getByIdForDoctor(1L, doctorId);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo(AppointmentStatus.ACCEPTED);
    }

    @Test
    void getByIdForDoctor_throwsNotFound_whenAppointmentMissing() {
        when(repository.findByIdAndDoctorId(eq(77L), eq(doctorId))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByIdForDoctor(77L, doctorId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("77");
    }

    private Appointment appointment(AppointmentStatus status) {
        return Appointment.builder()
                .id(1L)
                .patientId(patientId)
                .doctorId(doctorId)
                .status(status)
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