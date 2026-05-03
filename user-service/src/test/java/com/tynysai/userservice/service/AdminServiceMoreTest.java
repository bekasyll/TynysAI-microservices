package com.tynysai.userservice.service;

import com.tynysai.common.dto.PageResponse;
import com.tynysai.userservice.dto.response.AdminStatsResponse;
import com.tynysai.userservice.dto.response.DoctorProfileResponse;
import com.tynysai.userservice.dto.response.UserResponse;
import com.tynysai.userservice.model.DoctorProfile;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceMoreTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private DoctorProfileRepository doctorProfileRepository;
    @Mock
    private PatientProfileRepository patientProfileRepository;
    @Mock
    private UserService userService;
    @Mock
    private DoctorProfileService doctorProfileService;
    @Mock
    private KeycloakAdminClient keycloak;

    @InjectMocks
    private AdminService service;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder()
                .id(userId)
                .email("u@x.kz")
                .firstName("F")
                .lastName("L")
                .phoneNumber("+77011112222")
                .role(Role.DOCTOR)
                .enabled(true)
                .build();
    }

    @Test
    void getStats_aggregatesCounters() {
        when(userRepository.count()).thenReturn(10L);
        when(userRepository.countByRole(Role.PATIENT)).thenReturn(7L);
        when(userRepository.countByRole(Role.DOCTOR)).thenReturn(2L);
        when(userRepository.countByRoleAndEnabled(Role.PATIENT, true)).thenReturn(5L);
        when(userRepository.countByRoleAndEnabled(Role.DOCTOR, true)).thenReturn(1L);
        when(doctorProfileRepository.countByApproved(false)).thenReturn(3L);

        AdminStatsResponse stats = service.getStats();

        assertThat(stats.getTotalUsers()).isEqualTo(10);
        assertThat(stats.getTotalPatients()).isEqualTo(7);
        assertThat(stats.getTotalDoctors()).isEqualTo(2);
        assertThat(stats.getActivePatients()).isEqualTo(5);
        assertThat(stats.getActiveDoctors()).isEqualTo(1);
        assertThat(stats.getPendingDoctorApprovals()).isEqualTo(3);
    }

    @Test
    void getAllUsers_mapsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(user), pageable, 1));

        PageResponse<UserResponse> resp = service.getAllUsers(pageable);

        assertThat(resp.getContent()).hasSize(1);
        assertThat(resp.getContent().get(0).getEmail()).isEqualTo("u@x.kz");
    }

    @Test
    void getUsersByRole_mapsPage() {
        Pageable pageable = PageRequest.of(0, 5);
        when(userRepository.findByRole(Role.DOCTOR, pageable))
                .thenReturn(new PageImpl<>(List.of(user), pageable, 1));

        PageResponse<UserResponse> resp = service.getUsersByRole(Role.DOCTOR, pageable);

        assertThat(resp.getContent()).hasSize(1);
    }

    @Test
    void searchUsers_usesRoleSearch_whenRoleProvided() {
        Pageable pageable = PageRequest.of(0, 5);
        when(userRepository.searchByRoleAndName(Role.DOCTOR, "ann", pageable))
                .thenReturn(new PageImpl<>(List.of(user), pageable, 1));

        PageResponse<UserResponse> resp = service.searchUsers(Role.DOCTOR, "ann", pageable);

        assertThat(resp.getContent()).hasSize(1);
        verify(userRepository, never()).searchByName(any(), any());
    }

    @Test
    void searchUsers_usesPlainSearch_whenRoleNull() {
        Pageable pageable = PageRequest.of(0, 5);
        when(userRepository.searchByName("ann", pageable))
                .thenReturn(new PageImpl<>(List.of(user), pageable, 1));

        PageResponse<UserResponse> resp = service.searchUsers(null, "ann", pageable);

        assertThat(resp.getContent()).hasSize(1);
    }

    @Test
    void getPendingDoctors_appliesDefaultSort_whenPageableUnsorted() {
        Pageable pageable = PageRequest.of(0, 10);
        DoctorProfile profile = DoctorProfile.builder().userId(userId)
                .specialization("Card").approved(false).build();
        Page<DoctorProfile> page = new PageImpl<>(List.of(profile), pageable, 1);
        when(doctorProfileRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);
        when(userService.findById(userId)).thenReturn(user);

        PageResponse<DoctorProfileResponse> resp = service.getPendingDoctors(null, pageable);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(doctorProfileRepository).findAll(any(Specification.class), captor.capture());
        assertThat(captor.getValue().getSort()).isNotEqualTo(Sort.unsorted());
        assertThat(resp.getContent()).hasSize(1);
    }

    @Test
    void getPendingDoctors_keepsCallerSort_whenAlreadySorted() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("id"));
        when(doctorProfileRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        service.getPendingDoctors(null, pageable);

        verify(doctorProfileRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void getPendingDoctors_addsUserIdFilter_whenQueryProvided() {
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.searchByRoleAndName(eq(Role.DOCTOR), eq("ann"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(user)));
        when(doctorProfileRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        service.getPendingDoctors("ann", pageable);

        verify(userRepository).searchByRoleAndName(eq(Role.DOCTOR), eq("ann"), any(Pageable.class));
    }

    @Test
    void approveDoctor_delegatesToProfileService() {
        DoctorProfileResponse data = DoctorProfileResponse.builder().userId(userId).approved(true).build();
        when(doctorProfileService.approve(userId, true)).thenReturn(data);

        DoctorProfileResponse resp = service.approveDoctor(userId);

        assertThat(resp).isSameAs(data);
    }

    @Test
    void toggleStatus_flipsEnabledFlag() {
        when(userService.findById(userId)).thenReturn(user);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse resp = service.toggleStatus(userId);

        verify(keycloak).setUserEnabled(userId, false);
        assertThat(user.isEnabled()).isFalse();
        assertThat(resp.isEnabled()).isFalse();
    }

    @Test
    void toggleStatus_canEnableDisabledUser() {
        user.setEnabled(false);
        when(userService.findById(userId)).thenReturn(user);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        service.toggleStatus(userId);

        verify(keycloak).setUserEnabled(userId, true);
        assertThat(user.isEnabled()).isTrue();
    }

    @Test
    void logoutSessions_callsKeycloak() {
        when(userService.findById(userId)).thenReturn(user);

        service.logoutSessions(userId);

        verify(keycloak).logoutAllSessions(userId);
    }
}