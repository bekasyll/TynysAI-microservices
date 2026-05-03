package com.tynysai.common.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {
    @Test
    void successWithData_setsSuccessTrueAndCarriesData() {
        ApiResponse<Integer> r = ApiResponse.success(42);

        assertThat(r.isSuccess()).isTrue();
        assertThat(r.getData()).isEqualTo(42);
        assertThat(r.getMessage()).isNull();
        assertThat(r.getTimestamp()).isNotNull();
    }

    @Test
    void successWithMessageAndData_setsBoth() {
        ApiResponse<Integer> r = ApiResponse.success("ok", 42);

        assertThat(r.isSuccess()).isTrue();
        assertThat(r.getMessage()).isEqualTo("ok");
        assertThat(r.getData()).isEqualTo(42);
    }

    @Test
    void successWithMessageOnly_hasNullData() {
        ApiResponse<Void> r = ApiResponse.success("done");

        assertThat(r.isSuccess()).isTrue();
        assertThat(r.getMessage()).isEqualTo("done");
        assertThat(r.getData()).isNull();
    }

    @Test
    void error_setsSuccessFalseAndCarriesMessage() {
        ApiResponse<Object> r = ApiResponse.error("boom");

        assertThat(r.isSuccess()).isFalse();
        assertThat(r.getMessage()).isEqualTo("boom");
        assertThat(r.getData()).isNull();
    }

    @Test
    void timestamp_isStampedAtCreation() {
        LocalDateTime before = LocalDateTime.now();
        ApiResponse<Integer> r = ApiResponse.success(1);
        LocalDateTime after = LocalDateTime.now();

        assertThat(r.getTimestamp())
                .isAfterOrEqualTo(before)
                .isBeforeOrEqualTo(after);
    }
}