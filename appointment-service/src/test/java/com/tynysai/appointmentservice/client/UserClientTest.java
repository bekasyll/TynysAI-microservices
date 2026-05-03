package com.tynysai.appointmentservice.client;

import com.tynysai.appointmentservice.exception.ResourceNotFoundException;
import com.tynysai.common.client.dto.DoctorDto;
import com.tynysai.common.client.dto.UserDto;
import com.tynysai.common.dto.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserClientTest {
    @Mock
    private UserFeignClient feign;

    @InjectMocks
    private UserClient client;

    private UUID id;

    @BeforeEach
    void setUp() {
        id = UUID.randomUUID();
    }

    @Test
    void getById_returnsUser_whenFeignSucceeds() {
        UserDto user = new UserDto();
        user.setId(id);
        user.setEmail("a@b.kz");
        when(feign.getUserById(id)).thenReturn(ApiResponse.success(user));

        UserDto result = client.getById(id);

        assertThat(result).isSameAs(user);
    }

    @Test
    void getById_throwsNotFound_whenResponseIsNull() {
        when(feign.getUserById(id)).thenReturn(null);

        assertThatThrownBy(() -> client.getById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void getById_throwsNotFound_whenDataIsNull() {
        when(feign.getUserById(id)).thenReturn(ApiResponse.error("user-service is currently unavailable"));

        assertThatThrownBy(() -> client.getById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getById_translatesAnyException_intoResourceNotFound() {
        when(feign.getUserById(id)).thenThrow(new RuntimeException("connection refused"));

        assertThatThrownBy(() -> client.getById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void tryFetchUser_returnsNull_onAnyFailure() {
        when(feign.getUserById(id)).thenReturn(null);

        UserDto result = client.tryFetchUser(id);

        assertThat(result).isNull();
    }

    @Test
    void tryFetchUser_returnsUser_onSuccess() {
        UserDto user = new UserDto();
        user.setId(id);
        when(feign.getUserById(id)).thenReturn(ApiResponse.success(user));

        UserDto result = client.tryFetchUser(id);

        assertThat(result).isSameAs(user);
    }

    @Test
    void getDoctorSpecialization_returnsValue_onSuccess() {
        DoctorDto doctor = new DoctorDto();
        doctor.setUserId(id);
        doctor.setSpecialization("Pulmonology");
        when(feign.getDoctorById(id)).thenReturn(ApiResponse.success(doctor));

        String result = client.getDoctorSpecialization(id);

        assertThat(result).isEqualTo("Pulmonology");
    }

    @Test
    void getDoctorSpecialization_returnsNull_whenResponseIsNull() {
        when(feign.getDoctorById(id)).thenReturn(null);

        assertThat(client.getDoctorSpecialization(id)).isNull();
    }

    @Test
    void getDoctorSpecialization_returnsNull_whenDataIsNull() {
        when(feign.getDoctorById(id)).thenReturn(ApiResponse.error("user-service is currently unavailable"));

        assertThat(client.getDoctorSpecialization(id)).isNull();
    }

    @Test
    void getDoctorSpecialization_returnsNull_whenFeignThrows() {
        when(feign.getDoctorById(id)).thenThrow(new RuntimeException("network down"));

        assertThat(client.getDoctorSpecialization(id)).isNull();
    }
}
