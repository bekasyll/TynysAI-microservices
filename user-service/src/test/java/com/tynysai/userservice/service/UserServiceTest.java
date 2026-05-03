package com.tynysai.userservice.service;

import com.tynysai.userservice.dto.request.ChangePasswordRequest;
import com.tynysai.userservice.dto.request.UpdateUserRequest;
import com.tynysai.userservice.dto.response.UserResponse;
import com.tynysai.userservice.exception.BadRequestException;
import com.tynysai.userservice.exception.ResourceNotFoundException;
import com.tynysai.userservice.model.User;
import com.tynysai.userservice.model.enums.Role;
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
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private KeycloakAdminClient keycloak;

    @InjectMocks
    private UserService service;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder()
                .id(userId)
                .email("a@b.kz")
                .firstName("Aa")
                .lastName("Bb")
                .phoneNumber("+77011112222")
                .role(Role.PATIENT)
                .enabled(true)
                .emailVerified(true)
                .build();
    }

    @Test
    void findById_returnsUser_whenPresent() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        User result = service.findById(userId);

        assertThat(result).isSameAs(user);
    }

    @Test
    void findById_throwsNotFound_whenAbsent() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(userId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void searchUserIds_returnsEmpty_whenQueryBlank() {
        assertThat(service.searchUserIds(Role.DOCTOR, " ")).isEmpty();
        assertThat(service.searchUserIds(null, null)).isEmpty();
    }

    @Test
    void searchUserIds_usesRoleSearch_whenRoleProvided() {
        Page<User> page = new PageImpl<>(List.of(user));
        when(userRepository.searchByRoleAndName(eq(Role.PATIENT), eq("aa"), any(Pageable.class)))
                .thenReturn(page);

        List<UUID> ids = service.searchUserIds(Role.PATIENT, "aa");

        assertThat(ids).containsExactly(userId);
        verify(userRepository, never()).searchByName(any(), any());
    }

    @Test
    void searchUserIds_usesPlainSearch_whenRoleNull() {
        Page<User> page = new PageImpl<>(List.of(user));
        when(userRepository.searchByName(eq("aa"), any(Pageable.class))).thenReturn(page);

        List<UUID> ids = service.searchUserIds(null, "aa");

        assertThat(ids).containsExactly(userId);
        verify(userRepository, never()).searchByRoleAndName(any(), any(), any());
    }

    @Test
    void getById_returnsUserResponse() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserResponse response = service.getById(userId);

        assertThat(response.getId()).isEqualTo(userId);
        assertThat(response.getEmail()).isEqualTo("a@b.kz");
    }

    @Test
    void getByEmail_returnsResponse_whenPresent() {
        when(userRepository.findByEmail("a@b.kz")).thenReturn(Optional.of(user));

        UserResponse response = service.getByEmail("a@b.kz");

        assertThat(response.getEmail()).isEqualTo("a@b.kz");
    }

    @Test
    void getByEmail_throwsNotFound_whenAbsent() {
        when(userRepository.findByEmail("x@y.kz")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByEmail("x@y.kz"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getOrProvision_returnsExisting_whenFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserResponse resp = service.getOrProvision(userId, "x@y.kz", "X", "Y", List.of("PATIENT"));

        assertThat(resp.getEmail()).isEqualTo("a@b.kz");
        verify(userRepository, never()).save(any());
    }

    @Test
    void getOrProvision_provisionsAdmin_whenAdminRoleProvided() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse resp = service.getOrProvision(userId, "admin@x.kz", "A", "B",
                List.of("ADMIN", "DOCTOR"));

        assertThat(resp.getRole()).isEqualTo(Role.ADMIN);
        assertThat(resp.getEmail()).isEqualTo("admin@x.kz");
    }

    @Test
    void getOrProvision_provisionsDoctor_whenOnlyDoctorRolePresent() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse resp = service.getOrProvision(userId, "doc@x.kz", "D", "Doc",
                Set.of("doctor"));

        assertThat(resp.getRole()).isEqualTo(Role.DOCTOR);
    }

    @Test
    void getOrProvision_defaultsToPatient_whenRolesEmpty() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse resp = service.getOrProvision(userId, null, null, null, List.of());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getRole()).isEqualTo(Role.PATIENT);
        // email defaults to id when blank
        assertThat(saved.getEmail()).isEqualTo(userId.toString());
        assertThat(saved.getFirstName()).isEmpty();
        assertThat(saved.getLastName()).isEmpty();
        assertThat(saved.isEnabled()).isTrue();
        assertThat(saved.isEmailVerified()).isTrue();
        assertThat(resp.getRole()).isEqualTo(Role.PATIENT);
    }

    @Test
    void getOrProvision_defaultsToPatient_whenRolesNull() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse resp = service.getOrProvision(userId, "e@e.kz", "F", "L", null);

        assertThat(resp.getRole()).isEqualTo(Role.PATIENT);
    }

    @Test
    void update_appliesNonBlankFields_andSkipsBlanks() {
        UpdateUserRequest req = new UpdateUserRequest();
        req.setFirstName("New");
        req.setLastName("");
        req.setMiddleName(" ");
        req.setPhoneNumber("+77019998877");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse resp = service.update(userId, req);

        assertThat(resp.getFirstName()).isEqualTo("New");
        assertThat(resp.getLastName()).isEqualTo("Bb");
        assertThat(resp.getMiddleName()).isNull();
        assertThat(resp.getPhoneNumber()).isEqualTo("+77019998877");
    }

    @Test
    void update_setsMiddleName_whenProvided() {
        UpdateUserRequest req = new UpdateUserRequest();
        req.setMiddleName("Mid");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse resp = service.update(userId, req);

        assertThat(resp.getMiddleName()).isEqualTo("Mid");
    }

    @Test
    void uploadAvatar_storesFileAndPersistsPath() {
        MultipartFile file = new MockMultipartFile("avatar", "x.png", "image/png", new byte[]{1, 2});
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(fileStorageService.storeAvatar(file, userId)).thenReturn("path/avatar.png");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse resp = service.uploadAvatar(userId, file);

        assertThat(resp.getAvatarPath()).isEqualTo("path/avatar.png");
        assertThat(user.getAvatarPath()).isEqualTo("path/avatar.png");
    }

    @Test
    void changePassword_throwsBadRequest_whenCurrentPasswordWrong() {
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword("old");
        req.setNewPassword("newpassword");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(keycloak.verifyPassword("a@b.kz", "old")).thenReturn(false);

        assertThatThrownBy(() -> service.changePassword(userId, req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("incorrect");

        verify(keycloak, never()).resetPassword(any(), any(), anyBoolean());
    }

    @Test
    void changePassword_resetsPassword_whenCurrentPasswordCorrect() {
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword("old");
        req.setNewPassword("newpassword");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(keycloak.verifyPassword("a@b.kz", "old")).thenReturn(true);

        service.changePassword(userId, req);

        verify(keycloak).resetPassword(userId, "newpassword", false);
    }

    @Test
    void deleteAvatar_deletesAndClears_whenAvatarPresent() {
        user.setAvatarPath("path/avatar.png");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        service.deleteAvatar(userId);

        verify(fileStorageService).deleteFile("path/avatar.png");
        assertThat(user.getAvatarPath()).isNull();
        verify(userRepository).save(user);
    }

    @Test
    void deleteAvatar_isNoop_whenAvatarMissing() {
        user.setAvatarPath(null);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        service.deleteAvatar(userId);

        verify(fileStorageService, never()).deleteFile(any());
        verify(userRepository, never()).save(any());
    }

}
