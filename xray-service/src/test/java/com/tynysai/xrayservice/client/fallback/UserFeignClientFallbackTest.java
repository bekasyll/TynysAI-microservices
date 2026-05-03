package com.tynysai.xrayservice.client.fallback;

import com.tynysai.common.client.dto.UserDto;
import com.tynysai.common.dto.ApiResponse;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserFeignClientFallbackTest {

    private final UserFeignClientFallback fallback = new UserFeignClientFallback();

    @Test
    void getUserById_returnsErrorResponse_whenInvoked() {
        UUID id = UUID.randomUUID();

        ApiResponse<UserDto> response = fallback.getUserById(id);

        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getData()).isNull();
        assertThat(response.getMessage()).contains("user-service");
    }

    @Test
    void searchUserIds_returnsEmptyDataAndUnsuccessful_whenInvoked() {
        ApiResponse<List<UUID>> response = fallback.searchUserIds("PATIENT", "anything");

        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getData()).isNotNull().isEmpty();
        assertThat(response.getMessage()).contains("user-service");
    }

    @Test
    void searchUserIds_returnsEmptyList_evenWithNullArgs() {
        ApiResponse<List<UUID>> response = fallback.searchUserIds(null, null);

        assertThat(response.getData()).isEmpty();
    }
}
