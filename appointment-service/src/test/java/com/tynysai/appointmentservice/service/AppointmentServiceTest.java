package com.tynysai.appointmentservice.service;

import com.tynysai.appointmentservice.client.UserClient;
import com.tynysai.appointmentservice.client.XrayClient;
import com.tynysai.appointmentservice.dto.request.AppointmentDecisionRequest;
import com.tynysai.appointmentservice.dto.request.AppointmentRequest;
import com.tynysai.appointmentservice.exception.BadRequestException;
import com.tynysai.appointmentservice.exception.ResourceNotFoundException;
import com.tynysai.appointmentservice.kafka.NotificationEventPublisher;
import com.tynysai.appointmentservice.model.Appointment;
import com.tynysai.appointmentservice.model.enums.AppointmentStatus;
import com.tynysai.appointmentservice.repository.AppointmentRepository;
import com.tynysai.common.client.dto.UserDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {
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
    void book_throwsBadRequest_whenTargetUserIsNotDoctor() {
        UserDto fakeDoctor = userDto(doctorId, "Алмас", "PATIENT");
        when(userClient.getById(patientId)).thenReturn(patient);
        when(userClient.getById(doctorId)).thenReturn(fakeDoctor);

        AppointmentRequest req = req(LocalDateTime.now().plusDays(1));

        assertThatThrownBy(() -> service.book(patientId, req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not a doctor");
        verify(repository, never()).save(any());
    }

    @Test
    void book_throwsBadRequest_whenSlotIsTooSoon() {
        when(userClient.getById(patientId)).thenReturn(patient);
        when(userClient.getById(doctorId)).thenReturn(doctor);

        AppointmentRequest req = req(LocalDateTime.now().plusMinutes(5));

        assertThatThrownBy(() -> service.book(patientId, req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("too soon");
    }

    @Test
    void book_throwsBadRequest_onSlotConflict() {
        when(userClient.getById(patientId)).thenReturn(patient);
        when(userClient.getById(doctorId)).thenReturn(doctor);
        when(repository.existsConflict(eq(doctorId), any(), any())).thenReturn(true);

        AppointmentRequest req = req(LocalDateTime.now().plusDays(1));

        assertThatThrownBy(() -> service.book(patientId, req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already taken");
        verify(repository, never()).save(any());
    }

    @Test
    void book_throwsNotFound_whenXrayDoesNotBelongToPatient() {
        when(userClient.getById(patientId)).thenReturn(patient);
        when(userClient.getById(doctorId)).thenReturn(doctor);
        when(xrayClient.existsForPatient(99L, patientId)).thenReturn(false);

        AppointmentRequest req = req(LocalDateTime.now().plusDays(1));
        req.setXrayAnalysisId(99L);

        assertThatThrownBy(() -> service.book(patientId, req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void book_savesPendingAppointmentAndNotifiesDoctor_whenAllValid() {
        when(userClient.getById(patientId)).thenReturn(patient);
        when(userClient.getById(doctorId)).thenReturn(doctor);
        when(repository.existsConflict(any(), any(), any())).thenReturn(false);
        when(repository.save(any(Appointment.class))).thenAnswer(inv -> {
            Appointment a = inv.getArgument(0);
            a.setId(123L);
            return a;
        });

        AppointmentRequest req = req(LocalDateTime.now().plusDays(1));

        service.book(patientId, req);

        ArgumentCaptor<Appointment> captor = ArgumentCaptor.forClass(Appointment.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(AppointmentStatus.PENDING);
        assertThat(captor.getValue().getPatientId()).isEqualTo(patientId);
        assertThat(captor.getValue().getDoctorId()).isEqualTo(doctorId);
        verify(notificationPublisher).publish(eq(doctorId), eq("APPOINTMENT_REQUESTED"), eq("123"), any(), any());
    }

    @Test
    void accept_throwsNotFound_whenAppointmentMissing() {
        when(repository.findByIdAndDoctorId(7L, doctorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.accept(7L, doctorId, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void accept_throwsBadRequest_whenStatusIsNotPending() {
        Appointment a = appointment(AppointmentStatus.ACCEPTED);
        when(repository.findByIdAndDoctorId(1L, doctorId)).thenReturn(Optional.of(a));

        assertThatThrownBy(() -> service.accept(1L, doctorId, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("PENDING");
    }

    @Test
    void accept_throwsBadRequest_onAcceptedConflict() {
        Appointment a = appointment(AppointmentStatus.PENDING);
        a.setAppointmentDate(LocalDateTime.now().plusDays(1));
        when(repository.findByIdAndDoctorId(1L, doctorId)).thenReturn(Optional.of(a));
        when(repository.existsAcceptedConflict(eq(doctorId), eq(1L), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> service.accept(1L, doctorId, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already have an accepted");
    }

    @Test
    void accept_setsAcceptedAndNotifiesPatient_whenAllValid() {
        Appointment a = appointment(AppointmentStatus.PENDING);
        a.setAppointmentDate(LocalDateTime.now().plusDays(1));
        when(repository.findByIdAndDoctorId(1L, doctorId)).thenReturn(Optional.of(a));
        when(repository.existsAcceptedConflict(any(), anyLong(), any(), any())).thenReturn(false);
        when(repository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userClient.getById(doctorId)).thenReturn(doctor);
        when(userClient.tryFetchUser(any())).thenReturn(null);

        service.accept(1L, doctorId, null);

        assertThat(a.getStatus()).isEqualTo(AppointmentStatus.ACCEPTED);
        verify(notificationPublisher).publish(eq(patientId), eq("APPOINTMENT_ACCEPTED"), any(), any(), any());
    }

    @Test
    void reject_throwsBadRequest_whenStatusIsNotPending() {
        Appointment a = appointment(AppointmentStatus.ACCEPTED);
        when(repository.findByIdAndDoctorId(1L, doctorId)).thenReturn(Optional.of(a));

        assertThatThrownBy(() -> service.reject(1L, doctorId, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("PENDING");
    }

    @Test
    void reject_passesDoctorNotesAsReasonInNotification() {
        Appointment a = appointment(AppointmentStatus.PENDING);
        when(repository.findByIdAndDoctorId(1L, doctorId)).thenReturn(Optional.of(a));
        when(repository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userClient.getById(doctorId)).thenReturn(doctor);
        when(userClient.tryFetchUser(any())).thenReturn(null);

        AppointmentDecisionRequest req = new AppointmentDecisionRequest();
        req.setDoctorNotes("слот занят другой операцией");

        service.reject(1L, doctorId, req);

        assertThat(a.getStatus()).isEqualTo(AppointmentStatus.REJECTED);
        assertThat(a.getDoctorNotes()).isEqualTo("слот занят другой операцией");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<java.util.Map<String, String>> params = ArgumentCaptor.forClass(java.util.Map.class);
        verify(notificationPublisher).publish(eq(patientId), eq("APPOINTMENT_REJECTED"), any(), any(), params.capture());
        assertThat(params.getValue())
                .containsEntry("doctorName", "Dr. House")
                .containsEntry("reason", "слот занят другой операцией");
    }

    @Test
    void cancel_throwsBadRequest_whenAppointmentIsCompleted() {
        Appointment a = appointment(AppointmentStatus.COMPLETED);
        when(repository.findByIdAndPatientId(1L, patientId)).thenReturn(Optional.of(a));

        assertThatThrownBy(() -> service.cancel(1L, patientId))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Completed");
    }

    @Test
    void cancel_setsCancelledAndNotifiesDoctor() {
        Appointment a = appointment(AppointmentStatus.PENDING);
        when(repository.findByIdAndPatientId(1L, patientId)).thenReturn(Optional.of(a));
        when(repository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userClient.getById(patientId)).thenReturn(patient);
        when(userClient.tryFetchUser(any())).thenReturn(null);

        service.cancel(1L, patientId);

        assertThat(a.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        verify(notificationPublisher).publish(eq(doctorId), eq("APPOINTMENT_CANCELLED"), any(), any(), any());
    }

    @Test
    void complete_throwsBadRequest_whenStatusIsNotAccepted() {
        Appointment a = appointment(AppointmentStatus.PENDING);
        when(repository.findByIdAndDoctorId(1L, doctorId)).thenReturn(Optional.of(a));

        assertThatThrownBy(() -> service.complete(1L, doctorId, 555L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("ACCEPTED");
    }

    @Test
    void complete_setsCompletedAndStoresReportId() {
        Appointment a = appointment(AppointmentStatus.ACCEPTED);
        when(repository.findByIdAndDoctorId(1L, doctorId)).thenReturn(Optional.of(a));
        when(repository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userClient.getById(doctorId)).thenReturn(doctor);
        when(userClient.tryFetchUser(any())).thenReturn(null);

        service.complete(1L, doctorId, 555L);

        assertThat(a.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
        assertThat(a.getReportId()).isEqualTo(555L);
    }

    @Test
    void getBusySlots_roundsToNearestHalfHour_dedups_andSorts() {
        when(repository.findBookedSlots(eq(doctorId), any(), any())).thenReturn(List.of(
                LocalDateTime.of(2026, 5, 5, 10, 31),
                LocalDateTime.of(2026, 5, 5, 9, 14),
                LocalDateTime.of(2026, 5, 5, 9, 45),
                LocalDateTime.of(2026, 5, 5, 9, 14)
        ));

        List<String> slots = service.getBusySlots(doctorId, LocalDate.of(2026, 5, 5));

        assertThat(slots).containsExactly("09:00", "09:30", "10:30");
    }

    @Test
    void linkReport_setsReportId_andCompletesAcceptedAppointment() {
        Appointment a = appointment(AppointmentStatus.ACCEPTED);
        when(repository.findByIdAndDoctorId(1L, doctorId)).thenReturn(Optional.of(a));

        service.linkReport(1L, doctorId, 999L);

        assertThat(a.getReportId()).isEqualTo(999L);
        assertThat(a.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
        verify(repository).save(a);
    }

    @Test
    void linkReport_setsReportId_butDoesNotChangeStatusForNonAccepted() {
        Appointment a = appointment(AppointmentStatus.CANCELLED);
        when(repository.findByIdAndDoctorId(1L, doctorId)).thenReturn(Optional.of(a));

        service.linkReport(1L, doctorId, 999L);

        assertThat(a.getReportId()).isEqualTo(999L);
        assertThat(a.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
    }

    @Test
    void linkReport_doesNothing_whenAppointmentMissing() {
        when(repository.findByIdAndDoctorId(1L, doctorId)).thenReturn(Optional.empty());

        service.linkReport(1L, doctorId, 999L);

        verify(repository, never()).save(any());
    }

    private AppointmentRequest req(LocalDateTime when) {
        AppointmentRequest r = new AppointmentRequest();
        r.setDoctorId(doctorId);
        r.setAppointmentDate(when);
        return r;
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