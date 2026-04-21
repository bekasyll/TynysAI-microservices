package com.tynysai.appointmentservice.client;

import com.tynysai.appointmentservice.client.dto.DoctorDto;
import com.tynysai.appointmentservice.client.dto.UserDto;
import com.tynysai.appointmentservice.client.dto.WrappedResponse;
import com.tynysai.appointmentservice.exception.ResourceNotFoundException;
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

    public String getDoctorSpecialization(UUID doctorUserId) {
        try {
            WrappedResponse<DoctorDto> resp = userFeignClient.getDoctorById(doctorUserId);
            return resp != null && resp.getData() != null ? resp.getData().getSpecialization() : null;
        } catch (Exception e) {
            log.debug("Could not fetch doctor specialization for {}: {}", doctorUserId, e.getMessage());
            return null;
        }
    }
}
