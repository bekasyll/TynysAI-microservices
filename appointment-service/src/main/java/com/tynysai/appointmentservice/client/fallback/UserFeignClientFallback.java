package com.tynysai.appointmentservice.client.fallback;

import com.tynysai.appointmentservice.client.UserFeignClient;
import com.tynysai.common.client.dto.DoctorDto;
import com.tynysai.common.client.dto.UserDto;
import com.tynysai.common.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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
}
