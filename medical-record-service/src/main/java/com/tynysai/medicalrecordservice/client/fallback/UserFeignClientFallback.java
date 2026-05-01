package com.tynysai.medicalrecordservice.client.fallback;

import com.tynysai.common.client.dto.DoctorDto;
import com.tynysai.common.client.dto.UserDto;
import com.tynysai.common.dto.ApiResponse;
import com.tynysai.medicalrecordservice.client.UserFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class UserFeignClientFallback implements UserFeignClient {
    @Override
    public ApiResponse<UserDto> getUserById(UUID id) {
        log.warn("UserFeignClient fallback triggered for getUserById(id={})", id);
        return ApiResponse.error("user-service is currently unavailable");
    }

    @Override
    public ApiResponse<DoctorDto> getDoctorById(UUID id) {
        log.warn("UserFeignClient fallback triggered for getDoctorById(id={})", id);
        return ApiResponse.error("user-service is currently unavailable");
    }

    @Override
    public ApiResponse<List<UUID>> searchUserIds(String role, String q) {
        log.warn("UserFeignClient fallback for searchUserIds(role={}, q={})", role, q);
        return ApiResponse.<List<UUID>>builder()
                .success(false)
                .message("user-service is currently unavailable")
                .data(List.of())
                .build();
    }
}