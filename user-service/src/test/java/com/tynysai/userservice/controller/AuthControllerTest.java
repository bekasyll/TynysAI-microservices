package com.tynysai.userservice.controller;

import com.tynysai.common.dto.ApiResponse;
import com.tynysai.userservice.dto.request.ForgotPasswordRequest;
import com.tynysai.userservice.dto.request.RegisterPatientRequest;
import com.tynysai.userservice.dto.response.RegisterResponse;
import com.tynysai.userservice.model.enums.Role;
import com.tynysai.userservice.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {
    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController controller;

    @Test
    void registerPatient_returnsSuccessApiResponse() {
        RegisterPatientRequest req = new RegisterPatientRequest();
        req.setEmail("p@x.kz");
        UUID id = UUID.randomUUID();
        RegisterResponse data = RegisterResponse.builder()
                .userId(id).email("p@x.kz").role(Role.PATIENT).approved(true).build();
        when(authService.registerPatient(req)).thenReturn(data);

        ApiResponse<RegisterResponse> resp = controller.registerPatient(req);

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getData()).isSameAs(data);
        assertThat(resp.getMessage()).contains("Account created");
    }

    @Test
    void forgotPassword_normalizesEmailAndDelegates() {
        ForgotPasswordRequest req = new ForgotPasswordRequest();
        req.setEmail("  PAT@X.KZ ");

        ApiResponse<Void> resp = controller.forgotPassword(req);

        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        verify(authService).forgotPassword(emailCaptor.capture());
        assertThat(emailCaptor.getValue()).isEqualTo("pat@x.kz");
        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getMessage()).contains("reset link");
    }
}
