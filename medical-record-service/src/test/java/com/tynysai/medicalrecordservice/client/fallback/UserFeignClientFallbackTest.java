package com.tynysai.medicalrecordservice.client.fallback;

import com.tynysai.common.client.dto.DoctorDto;
import com.tynysai.common.client.dto.UserDto;
import com.tynysai.common.dto.ApiResponse;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserFeignClientFallbackTest {
    private final UserFeignClientFallback fallback = new UserFeignClientFallback();

    @Test
    void getUserById_returnsErrorResponse() {
        ApiResponse<UserDto> response = fallback.getUserById(UUID.randomUUID());

        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getData()).isNull();
        assertThat(response.getMessage()).contains("user-service");
    }

    @Test
    void getDoctorById_returnsErrorResponse() {
        ApiResponse<DoctorDto> response = fallback.getDoctorById(UUID.randomUUID());

        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getData()).isNull();
        assertThat(response.getMessage()).contains("user-service");
    }

    @Test
    void searchUserIds_returnsEmptyList_andUnsuccessful() {
        ApiResponse<List<UUID>> response = fallback.searchUserIds("PATIENT", "q");

        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getData()).isEmpty();
        assertThat(response.getMessage()).contains("user-service");
    }
}
