package com.tynysai.userservice.controller;

import com.tynysai.common.dto.ApiResponse;
import com.tynysai.userservice.dto.request.ChangePasswordRequest;
import com.tynysai.userservice.dto.request.UpdateUserRequest;
import com.tynysai.userservice.dto.response.UserResponse;
import com.tynysai.userservice.model.User;
import com.tynysai.userservice.model.enums.Role;
import com.tynysai.userservice.service.FileStorageService;
import com.tynysai.userservice.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;
    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private UserController controller;

    private Jwt buildJwt(UUID id, String email, String firstName, String lastName,
                         Map<String, Object> realmAccess) {
        Jwt.Builder b = Jwt.withTokenValue("tok")
                .header("alg", "none")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .subject(id.toString());
        if (email != null) b.claim("email", email);
        if (firstName != null) b.claim("given_name", firstName);
        if (lastName != null) b.claim("family_name", lastName);
        if (realmAccess != null) b.claim("realm_access", realmAccess);
        return b.build();
    }

    @Test
    void getCurrentUser_extractsClaimsAndDelegates() {
        UUID id = UUID.randomUUID();
        Jwt jwt = buildJwt(id, "u@x.kz", "First", "Last",
                Map.of("roles", List.of("PATIENT", 42)));
        UserResponse data = UserResponse.builder().id(id).build();
        when(userService.getOrProvision(eq(id), eq("u@x.kz"), eq("First"), eq("Last"),
                anyCollection())).thenReturn(data);

        ApiResponse<UserResponse> resp = controller.getCurrentUser(jwt);

        assertThat(resp.getData()).isSameAs(data);
        ArgumentCaptor<Collection<String>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(userService).getOrProvision(eq(id), eq("u@x.kz"), eq("First"), eq("Last"),
                captor.capture());
        assertThat(captor.getValue()).contains("PATIENT");
        // non-string roles are filtered out
        assertThat(captor.getValue()).hasSize(1);
    }

    @Test
    void getCurrentUser_handlesMissingRealmAccess() {
        UUID id = UUID.randomUUID();
        Jwt jwt = buildJwt(id, "u@x.kz", "F", "L", null);
        UserResponse data = UserResponse.builder().id(id).build();
        when(userService.getOrProvision(eq(id), eq("u@x.kz"), eq("F"), eq("L"),
                anyCollection())).thenReturn(data);

        ApiResponse<UserResponse> resp = controller.getCurrentUser(jwt);

        ArgumentCaptor<Collection<String>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(userService).getOrProvision(eq(id), eq("u@x.kz"), eq("F"), eq("L"), captor.capture());
        assertThat(captor.getValue()).isEmpty();
        assertThat(resp.getData()).isSameAs(data);
    }

    @Test
    void getCurrentUser_handlesRealmAccessWithoutRolesArray() {
        UUID id = UUID.randomUUID();
        Jwt jwt = buildJwt(id, "u@x.kz", "F", "L", Map.of("other", "value"));
        UserResponse data = UserResponse.builder().id(id).build();
        when(userService.getOrProvision(eq(id), eq("u@x.kz"), eq("F"), eq("L"),
                anyCollection())).thenReturn(data);

        controller.getCurrentUser(jwt);

        ArgumentCaptor<Collection<String>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(userService).getOrProvision(eq(id), eq("u@x.kz"), eq("F"), eq("L"), captor.capture());
        assertThat(captor.getValue()).isEmpty();
    }

    @Test
    void getById_delegates() {
        UUID id = UUID.randomUUID();
        UserResponse data = UserResponse.builder().id(id).build();
        when(userService.getById(id)).thenReturn(data);

        ApiResponse<UserResponse> resp = controller.getById(id);

        assertThat(resp.getData()).isSameAs(data);
    }

    @Test
    void getByEmail_delegates() {
        UserResponse data = UserResponse.builder().email("a@b.kz").build();
        when(userService.getByEmail("a@b.kz")).thenReturn(data);

        ApiResponse<UserResponse> resp = controller.getByEmail("a@b.kz");

        assertThat(resp.getData()).isSameAs(data);
    }

    @Test
    void searchIds_delegates() {
        UUID id = UUID.randomUUID();
        when(userService.searchUserIds(Role.DOCTOR, "ann")).thenReturn(List.of(id));

        ApiResponse<List<UUID>> resp = controller.searchIds(Role.DOCTOR, "ann");

        assertThat(resp.getData()).containsExactly(id);
    }

    @Test
    void updateCurrentUser_delegates() {
        UUID id = UUID.randomUUID();
        UpdateUserRequest req = new UpdateUserRequest();
        UserResponse data = UserResponse.builder().id(id).build();
        when(userService.update(id, req)).thenReturn(data);

        ApiResponse<UserResponse> resp = controller.updateCurrentUser(id, req);

        assertThat(resp.getData()).isSameAs(data);
        assertThat(resp.getMessage()).isEqualTo("User updated");
    }

    @Test
    void changePassword_delegatesAndReturnsSuccessMessage() {
        UUID id = UUID.randomUUID();
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword("a");
        req.setNewPassword("password1");

        ApiResponse<Void> resp = controller.changePassword(id, req);

        verify(userService).changePassword(id, req);
        assertThat(resp.getMessage()).isEqualTo("Password changed");
    }

    @Test
    void uploadAvatar_delegates() {
        UUID id = UUID.randomUUID();
        MultipartFile file = new MockMultipartFile("avatar", "x.png", "image/png", new byte[]{1});
        UserResponse data = UserResponse.builder().id(id).avatarPath("p/a.png").build();
        when(userService.uploadAvatar(id, file)).thenReturn(data);

        ApiResponse<UserResponse> resp = controller.uploadAvatar(id, file);

        assertThat(resp.getData()).isSameAs(data);
        assertThat(resp.getMessage()).contains("Avatar uploaded");
    }

    @Test
    void getAvatar_returnsNotFound_whenAvatarPathNull() {
        UUID id = UUID.randomUUID();
        User user = User.builder().id(id).email("a").firstName("F").lastName("L")
                .phoneNumber("+77011112222").role(Role.PATIENT).build();
        when(userService.findById(id)).thenReturn(user);

        ResponseEntity<byte[]> resp = controller.getAvatar(id);

        assertThat(resp.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void getAvatar_returnsPng_forPngExtension() {
        UUID id = UUID.randomUUID();
        User user = User.builder().id(id).email("a").firstName("F").lastName("L")
                .phoneNumber("+77011112222").role(Role.PATIENT).avatarPath("dir/avatar.png").build();
        when(userService.findById(id)).thenReturn(user);
        when(fileStorageService.loadFile("dir/avatar.png")).thenReturn(new byte[]{1, 2, 3});

        ResponseEntity<byte[]> resp = controller.getAvatar(id);

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_PNG);
        assertThat(resp.getBody()).containsExactly(1, 2, 3);
    }

    @Test
    void getAvatar_returnsJpeg_forJpgExtension() {
        UUID id = UUID.randomUUID();
        User user = User.builder().id(id).email("a").firstName("F").lastName("L")
                .phoneNumber("+77011112222").role(Role.PATIENT).avatarPath("dir/avatar.jpg").build();
        when(userService.findById(id)).thenReturn(user);
        when(fileStorageService.loadFile("dir/avatar.jpg")).thenReturn(new byte[]{9});

        ResponseEntity<byte[]> resp = controller.getAvatar(id);

        assertThat(resp.getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_JPEG);
    }

    @Test
    void deleteAvatar_delegates() {
        UUID id = UUID.randomUUID();

        ApiResponse<Void> resp = controller.deleteAvatar(id);

        verify(userService).deleteAvatar(id);
        assertThat(resp.getMessage()).contains("Avatar deleted");
    }

    private static <T> Collection<T> anyCollection() {
        return org.mockito.ArgumentMatchers.anyCollection();
    }

    private static <T> T eq(T value) {
        return org.mockito.ArgumentMatchers.eq(value);
    }
}
