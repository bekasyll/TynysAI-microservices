package com.tynysai.userservice.service;

import com.tynysai.userservice.dto.request.RegisterDoctorRequest;
import com.tynysai.userservice.dto.request.RegisterPatientRequest;
import com.tynysai.userservice.dto.response.RegisterResponse;
import com.tynysai.userservice.exception.BadRequestException;
import com.tynysai.userservice.kafka.NotificationEventPublisher;
import com.tynysai.userservice.model.DoctorProfile;
import com.tynysai.userservice.model.PatientProfile;
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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private KeycloakAdminClient keycloak;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PatientProfileRepository patientProfileRepository;
    @Mock
    private DoctorProfileRepository doctorProfileRepository;
    @Mock
    private NotificationEventPublisher notificationPublisher;

    @InjectMocks
    private AuthService service;

    private RegisterPatientRequest patientReq;
    private RegisterDoctorRequest doctorReq;
    private UUID newUserId;

    @BeforeEach
    void setUp() {
        newUserId = UUID.randomUUID();

        patientReq = new RegisterPatientRequest();
        patientReq.setEmail("p@x.kz");
        patientReq.setFirstName("Pat");
        patientReq.setLastName("Doe");
        patientReq.setPassword("password1");
        patientReq.setMiddleName("M");
        patientReq.setPhoneNumber("+77011112222");

        doctorReq = new RegisterDoctorRequest();
        doctorReq.setEmail("d@x.kz");
        doctorReq.setFirstName("Doc");
        doctorReq.setLastName("Smith");
        doctorReq.setPassword("password1");
        doctorReq.setSpecialization("Cardiology");
        doctorReq.setLicenseNumber("LIC-1");
        doctorReq.setHospitalName("Central");
        doctorReq.setDepartment("Cardio");
        doctorReq.setYearsOfExperience(5);
        doctorReq.setBio("Bio");
        doctorReq.setEducation("Edu");
    }

    @Test
    void registerPatient_throwsBadRequest_whenEmailAlreadyTaken() {
        when(userRepository.existsByEmail("p@x.kz")).thenReturn(true);

        assertThatThrownBy(() -> service.registerPatient(patientReq))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already registered");
        verify(keycloak, never()).createUser(any(), any(), any(), any());
    }

    @Test
    void registerPatient_persistsUserAndProfile_andReturnsResponse() {
        when(userRepository.existsByEmail("p@x.kz")).thenReturn(false);
        when(keycloak.createUser("p@x.kz", "Pat", "Doe", "password1")).thenReturn(newUserId);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        RegisterResponse resp = service.registerPatient(patientReq);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertThat(saved.getId()).isEqualTo(newUserId);
        assertThat(saved.getEmail()).isEqualTo("p@x.kz");
        assertThat(saved.getRole()).isEqualTo(Role.PATIENT);
        assertThat(saved.getMiddleName()).isEqualTo("M");
        assertThat(saved.getPhoneNumber()).isEqualTo("+77011112222");
        assertThat(saved.isEnabled()).isTrue();
        assertThat(saved.isEmailVerified()).isTrue();

        ArgumentCaptor<PatientProfile> profileCaptor = ArgumentCaptor.forClass(PatientProfile.class);
        verify(patientProfileRepository).save(profileCaptor.capture());
        assertThat(profileCaptor.getValue().getUserId()).isEqualTo(newUserId);

        assertThat(resp.getUserId()).isEqualTo(newUserId);
        assertThat(resp.getRole()).isEqualTo(Role.PATIENT);
        assertThat(resp.isApproved()).isTrue();
    }

    @Test
    void registerPatient_handlesBlankOptionalFields() {
        patientReq.setMiddleName(null);
        patientReq.setPhoneNumber(null);
        when(userRepository.existsByEmail("p@x.kz")).thenReturn(false);
        when(keycloak.createUser(anyString(), anyString(), anyString(), anyString())).thenReturn(newUserId);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        service.registerPatient(patientReq);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getMiddleName()).isEmpty();
        assertThat(captor.getValue().getPhoneNumber()).isEmpty();
    }

    @Test
    void registerPatient_rollsBackKeycloakUser_whenLocalSaveFails() {
        when(userRepository.existsByEmail("p@x.kz")).thenReturn(false);
        when(keycloak.createUser(anyString(), anyString(), anyString(), anyString())).thenReturn(newUserId);
        when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("db down"));

        assertThatThrownBy(() -> service.registerPatient(patientReq))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("db down");
        verify(keycloak).deleteUserQuietly(newUserId);
    }

    @Test
    void registerDoctor_throwsBadRequest_whenEmailTaken() {
        when(userRepository.existsByEmail("d@x.kz")).thenReturn(true);

        assertThatThrownBy(() -> service.registerDoctor(doctorReq))
                .isInstanceOf(BadRequestException.class);
        verify(keycloak, never()).createUser(any(), any(), any(), any());
    }

    @Test
    void registerDoctor_throwsBadRequest_whenLicenseTaken() {
        when(userRepository.existsByEmail("d@x.kz")).thenReturn(false);
        when(doctorProfileRepository.existsByLicenseNumber("LIC-1")).thenReturn(true);

        assertThatThrownBy(() -> service.registerDoctor(doctorReq))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("License");
        verify(keycloak, never()).createUser(any(), any(), any(), any());
    }

    @Test
    void registerDoctor_persistsUserProfileAndAssignsRole() {
        when(userRepository.existsByEmail("d@x.kz")).thenReturn(false);
        when(doctorProfileRepository.existsByLicenseNumber("LIC-1")).thenReturn(false);
        when(keycloak.createUser("d@x.kz", "Doc", "Smith", "password1")).thenReturn(newUserId);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findAllByRole(Role.ADMIN)).thenReturn(List.of());

        RegisterResponse resp = service.registerDoctor(doctorReq);

        verify(keycloak).assignRealmRole(newUserId, "DOCTOR");
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getRole()).isEqualTo(Role.DOCTOR);

        ArgumentCaptor<DoctorProfile> profileCaptor = ArgumentCaptor.forClass(DoctorProfile.class);
        verify(doctorProfileRepository).save(profileCaptor.capture());
        DoctorProfile profile = profileCaptor.getValue();
        assertThat(profile.getUserId()).isEqualTo(newUserId);
        assertThat(profile.getSpecialization()).isEqualTo("Cardiology");
        assertThat(profile.getLicenseNumber()).isEqualTo("LIC-1");
        assertThat(profile.getHospitalName()).isEqualTo("Central");
        assertThat(profile.getDepartment()).isEqualTo("Cardio");
        assertThat(profile.getYearsOfExperience()).isEqualTo(5);
        assertThat(profile.isApproved()).isFalse();
        assertThat(profile.getWorkSchedule()).isNotEmpty();

        assertThat(resp.getRole()).isEqualTo(Role.DOCTOR);
        assertThat(resp.isApproved()).isFalse();
    }

    @Test
    void registerDoctor_skipsLicenseCheck_whenLicenseBlank() {
        doctorReq.setLicenseNumber("");
        when(userRepository.existsByEmail("d@x.kz")).thenReturn(false);
        when(keycloak.createUser(anyString(), anyString(), anyString(), anyString())).thenReturn(newUserId);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findAllByRole(Role.ADMIN)).thenReturn(List.of());

        service.registerDoctor(doctorReq);

        verify(doctorProfileRepository, never()).existsByLicenseNumber(any());
    }

    @Test
    void registerDoctor_notifiesAdmins_whenAdminsExist() {
        when(userRepository.existsByEmail("d@x.kz")).thenReturn(false);
        when(doctorProfileRepository.existsByLicenseNumber("LIC-1")).thenReturn(false);
        when(keycloak.createUser(anyString(), anyString(), anyString(), anyString())).thenReturn(newUserId);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        UUID admin1 = UUID.randomUUID();
        UUID admin2 = UUID.randomUUID();
        when(userRepository.findAllByRole(Role.ADMIN)).thenReturn(List.of(
                User.builder().id(admin1).email("a1@x.kz").firstName("A").lastName("One").role(Role.ADMIN).build(),
                User.builder().id(admin2).email("a2@x.kz").firstName("A").lastName("Two").role(Role.ADMIN).build()
        ));

        service.registerDoctor(doctorReq);

        verify(notificationPublisher).publish(eq(admin1), eq("DOCTOR_PENDING_APPROVAL"),
                eq(newUserId.toString()), eq("DOCTOR_PROFILE"), any(Map.class));
        verify(notificationPublisher).publish(eq(admin2), eq("DOCTOR_PENDING_APPROVAL"),
                eq(newUserId.toString()), eq("DOCTOR_PROFILE"), any(Map.class));
    }

    @Test
    void registerDoctor_swallowsNotificationExceptions() {
        when(userRepository.existsByEmail("d@x.kz")).thenReturn(false);
        when(doctorProfileRepository.existsByLicenseNumber("LIC-1")).thenReturn(false);
        when(keycloak.createUser(anyString(), anyString(), anyString(), anyString())).thenReturn(newUserId);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        UUID admin = UUID.randomUUID();
        when(userRepository.findAllByRole(Role.ADMIN)).thenReturn(List.of(
                User.builder().id(admin).email("a@x.kz").firstName("A").lastName("Z").role(Role.ADMIN).build()
        ));
        doThrow(new RuntimeException("kafka down"))
                .when(notificationPublisher).publish(any(), any(), any(), any(), any());

        // Should not propagate
        RegisterResponse resp = service.registerDoctor(doctorReq);

        assertThat(resp.getRole()).isEqualTo(Role.DOCTOR);
    }

    @Test
    void registerDoctor_rollsBackKeycloakUser_whenLocalSaveFails() {
        when(userRepository.existsByEmail("d@x.kz")).thenReturn(false);
        when(doctorProfileRepository.existsByLicenseNumber("LIC-1")).thenReturn(false);
        when(keycloak.createUser(anyString(), anyString(), anyString(), anyString())).thenReturn(newUserId);
        when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("db boom"));

        assertThatThrownBy(() -> service.registerDoctor(doctorReq))
                .isInstanceOf(RuntimeException.class);
        verify(keycloak).deleteUserQuietly(newUserId);
    }

    @Test
    void forgotPassword_isNoop_whenUserMissing() {
        when(userRepository.findByEmail("nope@x.kz")).thenReturn(Optional.empty());

        service.forgotPassword("nope@x.kz");

        verify(keycloak, never()).sendExecuteActionsEmail(any(), any());
    }

    @Test
    void forgotPassword_isNoop_whenUserDisabled() {
        User disabled = User.builder()
                .id(newUserId).email("p@x.kz").firstName("P").lastName("D")
                .role(Role.PATIENT).enabled(false).build();
        when(userRepository.findByEmail("p@x.kz")).thenReturn(Optional.of(disabled));

        service.forgotPassword("p@x.kz");

        verify(keycloak, never()).sendExecuteActionsEmail(any(), any());
    }

    @Test
    void forgotPassword_sendsExecuteActionsEmail_whenUserActive() {
        User active = User.builder()
                .id(newUserId).email("p@x.kz").firstName("P").lastName("D")
                .role(Role.PATIENT).enabled(true).build();
        when(userRepository.findByEmail("p@x.kz")).thenReturn(Optional.of(active));

        service.forgotPassword("p@x.kz");

        verify(keycloak, times(1)).sendExecuteActionsEmail(eq(newUserId), eq(List.of("UPDATE_PASSWORD")));
    }

    @Test
    void forgotPassword_swallowsKeycloakErrors() {
        User active = User.builder()
                .id(newUserId).email("p@x.kz").firstName("P").lastName("D")
                .role(Role.PATIENT).enabled(true).build();
        when(userRepository.findByEmail("p@x.kz")).thenReturn(Optional.of(active));
        doThrow(new RuntimeException("smtp down"))
                .when(keycloak).sendExecuteActionsEmail(any(), any());

        // Should not propagate
        service.forgotPassword("p@x.kz");
    }
}
