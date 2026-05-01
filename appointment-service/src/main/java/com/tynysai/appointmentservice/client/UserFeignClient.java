package com.tynysai.appointmentservice.client;

import com.tynysai.appointmentservice.client.fallback.UserFeignClientFallback;
import com.tynysai.common.client.dto.DoctorDto;
import com.tynysai.common.client.dto.UserDto;
import com.tynysai.common.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "user-service", fallback = UserFeignClientFallback.class)
public interface UserFeignClient {
    @GetMapping("/api/users/{id}")
    ApiResponse<UserDto> getUserById(@PathVariable("id") UUID id);

    @GetMapping("/api/doctors/{id}")
    ApiResponse<DoctorDto> getDoctorById(@PathVariable("id") UUID id);
}
