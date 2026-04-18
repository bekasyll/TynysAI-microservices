package com.tynysai.medicalrecordservice.client;

import com.tynysai.medicalrecordservice.client.dto.DoctorDto;
import com.tynysai.medicalrecordservice.client.dto.UserDto;
import com.tynysai.medicalrecordservice.client.dto.WrappedResponse;
import com.tynysai.medicalrecordservice.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserClient {
    private final UserFeignClient userFeignClient;

    public UserDto getById(Long userId) {
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

    public UserDto tryGetById(Long userId) {
        try {
            return getById(userId);
        } catch (Exception e) {
            return null;
        }
    }

    public String getDoctorSpecialization(Long doctorUserId) {
        try {
            WrappedResponse<DoctorDto> resp = userFeignClient.getDoctorById(doctorUserId);
            return resp != null && resp.getData() != null ? resp.getData().getSpecialization() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
