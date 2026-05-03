package com.tynysai.userservice.service;

import com.tynysai.userservice.exception.BadRequestException;
import com.tynysai.userservice.exception.ResourceNotFoundException;
import com.tynysai.userservice.model.DoctorProfile;
import com.tynysai.userservice.model.TimeRange;
import com.tynysai.userservice.model.User;
import com.tynysai.userservice.model.enums.Role;
import com.tynysai.userservice.repository.DoctorProfileRepository;
import com.tynysai.userservice.repository.PatientProfileRepository;
import com.tynysai.userservice.repository.UserRepository;
import com.tynysai.userservice.security.KeycloakAdminClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private DoctorProfileRepository doctorProfileRepository;
    @Mock
    private PatientProfileRepository patientProfileRepository;
    @Mock
    private UserService userService;
    @Mock
    private KeycloakAdminClient keycloak;

    @InjectMocks
    private AdminService service;

    private UUID doctorId;
    private DoctorProfile profile;
    private User doctorUser;

    @BeforeEach
    void setUp() {
        doctorId = UUID.randomUUID();
        profile = DoctorProfile.builder().userId(doctorId).build();
        doctorUser = User.builder().id(doctorId).email("doc@x.kz").role(Role.DOCTOR).build();
    }

    @Test
    void updateDoctorWorkSchedule_throwsNotFound_whenProfileMissing() {
        when(doctorProfileRepository.findByUserId(doctorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateDoctorWorkSchedule(doctorId, Map.of()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateDoctorWorkSchedule_acceptsNullSchedule() {
        when(doctorProfileRepository.findByUserId(doctorId)).thenReturn(Optional.of(profile));
        when(userService.findById(doctorId)).thenReturn(doctorUser);
        when(doctorProfileRepository.save(any(DoctorProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        service.updateDoctorWorkSchedule(doctorId, null);

        assertThat(profile.getWorkSchedule()).isNull();
    }

    @Test
    void updateDoctorWorkSchedule_throwsBadRequest_whenStartIsNull() {
        Map<DayOfWeek, List<TimeRange>> bad = scheduleOf(DayOfWeek.MONDAY,
                new TimeRange(null, LocalTime.of(13, 0)));
        when(doctorProfileRepository.findByUserId(doctorId)).thenReturn(Optional.of(profile));

        assertThatThrownBy(() -> service.updateDoctorWorkSchedule(doctorId, bad))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("start and end are required");
    }

    @Test
    void updateDoctorWorkSchedule_throwsBadRequest_whenEndIsNotAfterStart() {
        Map<DayOfWeek, List<TimeRange>> bad = scheduleOf(DayOfWeek.MONDAY,
                new TimeRange(LocalTime.of(13, 0), LocalTime.of(13, 0)));
        when(doctorProfileRepository.findByUserId(doctorId)).thenReturn(Optional.of(profile));

        assertThatThrownBy(() -> service.updateDoctorWorkSchedule(doctorId, bad))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("end must be after start");
    }

    @Test
    void updateDoctorWorkSchedule_throwsBadRequest_whenIntervalsOverlap() {
        Map<DayOfWeek, List<TimeRange>> bad = scheduleOf(DayOfWeek.MONDAY,
                new TimeRange(LocalTime.of(9, 0), LocalTime.of(13, 0)),
                new TimeRange(LocalTime.of(12, 30), LocalTime.of(15, 0))); // overlaps morning shift
        when(doctorProfileRepository.findByUserId(doctorId)).thenReturn(Optional.of(profile));

        assertThatThrownBy(() -> service.updateDoctorWorkSchedule(doctorId, bad))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("intervals overlap");
    }

    @Test
    void updateDoctorWorkSchedule_acceptsAdjacentIntervals() {
        Map<DayOfWeek, List<TimeRange>> ok = scheduleOf(DayOfWeek.MONDAY,
                new TimeRange(LocalTime.of(13, 0), LocalTime.of(14, 0)),
                new TimeRange(LocalTime.of(14, 0), LocalTime.of(15, 0)));
        when(doctorProfileRepository.findByUserId(doctorId)).thenReturn(Optional.of(profile));
        when(userService.findById(doctorId)).thenReturn(doctorUser);
        when(doctorProfileRepository.save(any(DoctorProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        service.updateDoctorWorkSchedule(doctorId, ok);

        assertThat(profile.getWorkSchedule().get(DayOfWeek.MONDAY)).hasSize(2);
    }

    @Test
    void updateDoctorWorkSchedule_sortsIntervalsByStartTime() {
        Map<DayOfWeek, List<TimeRange>> unsorted = scheduleOf(DayOfWeek.MONDAY,
                new TimeRange(LocalTime.of(14, 0), LocalTime.of(18, 0)),
                new TimeRange(LocalTime.of(9, 0), LocalTime.of(13, 0)));
        when(doctorProfileRepository.findByUserId(doctorId)).thenReturn(Optional.of(profile));
        when(userService.findById(doctorId)).thenReturn(doctorUser);
        when(doctorProfileRepository.save(any(DoctorProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        service.updateDoctorWorkSchedule(doctorId, unsorted);

        ArgumentCaptor<DoctorProfile> captor = ArgumentCaptor.forClass(DoctorProfile.class);
        verify(doctorProfileRepository).save(captor.capture());
        List<TimeRange> sorted = captor.getValue().getWorkSchedule().get(DayOfWeek.MONDAY);
        assertThat(sorted.get(0).getStart()).isEqualTo(LocalTime.of(9, 0));
        assertThat(sorted.get(1).getStart()).isEqualTo(LocalTime.of(14, 0));
    }

    @Test
    void updateDoctorWorkSchedule_skipsValidationForEmptyDays() {
        // Days with empty/null lists are valid (means "off" that day).
        Map<DayOfWeek, List<TimeRange>> sparse = new HashMap<>();
        sparse.put(DayOfWeek.SATURDAY, List.of());
        sparse.put(DayOfWeek.SUNDAY, null);
        when(doctorProfileRepository.findByUserId(doctorId)).thenReturn(Optional.of(profile));
        when(userService.findById(doctorId)).thenReturn(doctorUser);
        when(doctorProfileRepository.save(any(DoctorProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        service.updateDoctorWorkSchedule(doctorId, sparse);

        assertThat(profile.getWorkSchedule()).isSameAs(sparse);
    }

    @Test
    void deleteUser_throwsBadRequest_whenTargetIsAdmin() {
        UUID adminId = UUID.randomUUID();
        when(userService.findById(adminId)).thenReturn(
                User.builder().id(adminId).role(Role.ADMIN).email("a@x.kz").build());

        assertThatThrownBy(() -> service.deleteUser(adminId))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Admin");

        verify(userRepository, never()).delete(any());
        verify(keycloak, never()).deleteUser(any());
    }

    @Test
    void deleteUser_proceedsWithLocalCleanup_whenKeycloakReturns404() {
        when(userService.findById(doctorId)).thenReturn(doctorUser);
        doThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found",
                org.springframework.http.HttpHeaders.EMPTY, new byte[0], null))
                .when(keycloak).deleteUser(doctorId);

        service.deleteUser(doctorId);

        verify(doctorProfileRepository).deleteByUserId(doctorId);
        verify(patientProfileRepository).deleteByUserId(doctorId);
        verify(userRepository).delete(doctorUser);
    }

    @Test
    void deleteUser_propagatesNonNotFoundKeycloakErrors() {
        when(userService.findById(doctorId)).thenReturn(doctorUser);
        doThrow(HttpClientErrorException.create(HttpStatus.INTERNAL_SERVER_ERROR, "Boom",
                org.springframework.http.HttpHeaders.EMPTY, new byte[0], null))
                .when(keycloak).deleteUser(doctorId);

        assertThatThrownBy(() -> service.deleteUser(doctorId))
                .isInstanceOf(HttpClientErrorException.class);

        verify(userRepository, never()).delete(any());
    }

    private Map<DayOfWeek, List<TimeRange>> scheduleOf(DayOfWeek day, TimeRange... ranges) {
        Map<DayOfWeek, List<TimeRange>> m = new HashMap<>();
        m.put(day, new java.util.ArrayList<>(List.of(ranges)));
        return m;
    }
}