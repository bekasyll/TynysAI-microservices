package com.tynysai.xrayservice.client.fallback;

import com.tynysai.xrayservice.client.UserFeignClient;
import com.tynysai.xrayservice.client.dto.UserDto;
import com.tynysai.xrayservice.client.dto.WrappedResponse;
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
}