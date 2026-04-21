package com.tynysai.xrayservice.client;

import com.tynysai.xrayservice.client.dto.UserDto;
import com.tynysai.xrayservice.client.dto.WrappedResponse;
import com.tynysai.xrayservice.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserClient {
    private final UserFeignClient userFeignClient;

    public UserDto getById(UUID userId) {
        try {
            WrappedResponse<UserDto> resp = userFeignClient.getUserById(userId);
            if (resp == null || resp.getData() == null) {
                throw new ResourceNotFoundException("User", "id", userId);
            }
            return resp.getData();
        } catch (Exception e) {
            log.warn("Failed to fetch user {}: {}", userId, e.getMessage());
            throw new ResourceNotFoundException("User", "id", userId);
        }
    }

    public UserDto tryFetchUser(UUID id) {
        try {
            return getById(id);
        } catch (Exception e) {
            return null;
        }
    }
}
