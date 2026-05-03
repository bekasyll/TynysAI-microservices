package com.tynysai.medicalrecordservice.client;

import com.tynysai.common.client.dto.DoctorDto;
import com.tynysai.common.client.dto.UserDto;
import com.tynysai.common.dto.ApiResponse;
import com.tynysai.medicalrecordservice.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
    void tryGetById_returnsNull_onAnyFailure() {
        when(feign.getUserById(id)).thenReturn(null);

        UserDto result = client.tryGetById(id);

        assertThat(result).isNull();
    }

    @Test
    void tryGetById_returnsUser_onSuccess() {
        UserDto user = new UserDto();
        user.setId(id);
        when(feign.getUserById(id)).thenReturn(ApiResponse.success(user));

        UserDto result = client.tryGetById(id);

        assertThat(result).isSameAs(user);
    }

    @Test
    void getDoctorSpecialization_returnsSpecialization_whenSuccess() {
        DoctorDto doctor = new DoctorDto();
        doctor.setUserId(id);
        doctor.setSpecialization("Cardiology");
        when(feign.getDoctorById(id)).thenReturn(ApiResponse.success(doctor));

        String result = client.getDoctorSpecialization(id);

        assertThat(result).isEqualTo("Cardiology");
    }

    @Test
    void getDoctorSpecialization_returnsNull_whenResponseIsNull() {
        when(feign.getDoctorById(id)).thenReturn(null);

        assertThat(client.getDoctorSpecialization(id)).isNull();
    }

    @Test
    void getDoctorSpecialization_returnsNull_whenDataIsNull() {
        when(feign.getDoctorById(id)).thenReturn(ApiResponse.error("err"));

        assertThat(client.getDoctorSpecialization(id)).isNull();
    }

    @Test
    void getDoctorSpecialization_returnsNull_whenFeignThrows() {
        when(feign.getDoctorById(id)).thenThrow(new RuntimeException("network down"));

        assertThat(client.getDoctorSpecialization(id)).isNull();
    }

    @Test
    void searchPatientIds_returnsEmpty_whenQueryIsBlank() {
        assertThat(client.searchPatientIds("")).isEmpty();
        assertThat(client.searchPatientIds("   ")).isEmpty();
        assertThat(client.searchPatientIds(null)).isEmpty();
    }

    @Test
    void searchPatientIds_returnsIds_onSuccess() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        when(feign.searchUserIds("PATIENT", "иван"))
                .thenReturn(ApiResponse.<List<UUID>>builder().success(true).data(List.of(a, b)).build());

        List<UUID> result = client.searchPatientIds("иван");

        assertThat(result).containsExactly(a, b);
    }

    @Test
    void searchPatientIds_returnsEmpty_whenFeignThrows() {
        when(feign.searchUserIds(any(), any())).thenThrow(new RuntimeException("network down"));

        assertThat(client.searchPatientIds("anything")).isEmpty();
    }

    @Test
    void searchPatientIds_returnsEmpty_whenDataNull() {
        when(feign.searchUserIds("PATIENT", "x"))
                .thenReturn(ApiResponse.error("user-service is currently unavailable"));

        assertThat(client.searchPatientIds("x")).isEmpty();
    }
}
