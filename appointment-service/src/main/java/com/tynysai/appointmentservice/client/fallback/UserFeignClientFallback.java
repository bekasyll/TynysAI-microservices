package com.tynysai.appointmentservice.client.fallback;

import com.tynysai.appointmentservice.client.UserFeignClient;
import com.tynysai.appointmentservice.client.dto.DoctorDto;
import com.tynysai.appointmentservice.client.dto.UserDto;
import com.tynysai.appointmentservice.client.dto.WrappedResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class UserFeignClientFallback implements UserFeignClient {
    @Override
    public WrappedResponse<UserDto> getUserById(UUID id) {
        log.warn("UserFeignClient fallback triggered for getUserById(id={})", id);
        return WrappedResponse.<UserDto>builder()
                .success(false)
                .message("user-service is currently unavailable")
                .data(null)
                .build();
    }

    @Override
    public WrappedResponse<DoctorDto> getDoctorById(UUID id) {
        log.warn("UserFeignClient fallback triggered for getDoctorById(id={})", id);
        return WrappedResponse.<DoctorDto>builder()
                .success(false)
                .message("user-service is currently unavailable")
                .data(null)
                .build();
    }
}
