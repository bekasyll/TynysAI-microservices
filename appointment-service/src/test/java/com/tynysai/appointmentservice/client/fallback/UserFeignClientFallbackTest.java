package com.tynysai.appointmentservice.client.fallback;

import com.tynysai.common.client.dto.DoctorDto;
import com.tynysai.common.client.dto.UserDto;
import com.tynysai.common.dto.ApiResponse;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserFeignClientFallbackTest {

    private final UserFeignClientFallback fallback = new UserFeignClientFallback();

    @Test
    void getUserById_returnsErrorResponse_indicatingServiceUnavailable() {
        ApiResponse<UserDto> resp = fallback.getUserById(UUID.randomUUID());

        assertThat(resp).isNotNull();
        assertThat(resp.isSuccess()).isFalse();
        assertThat(resp.getMessage()).contains("user-service");
        assertThat(resp.getData()).isNull();
    }

    @Test
    void getDoctorById_returnsErrorResponse_indicatingServiceUnavailable() {
        ApiResponse<DoctorDto> resp = fallback.getDoctorById(UUID.randomUUID());

        assertThat(resp).isNotNull();
        assertThat(resp.isSuccess()).isFalse();
        assertThat(resp.getMessage()).contains("user-service");
        assertThat(resp.getData()).isNull();
    }
}
