package com.tynysai.appointmentservice.client;

import com.tynysai.appointmentservice.client.dto.DoctorDto;
import com.tynysai.appointmentservice.client.dto.UserDto;
import com.tynysai.appointmentservice.client.dto.WrappedResponse;
import com.tynysai.appointmentservice.client.fallback.UserFeignClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", url = "${services.user-service.url}", fallback = UserFeignClientFallback.class)
public interface UserFeignClient {
    @GetMapping("/api/users/{id}")
    WrappedResponse<UserDto> getUserById(@PathVariable("id") Long id);

    @GetMapping("/api/doctors/{id}")
    WrappedResponse<DoctorDto> getDoctorById(@PathVariable("id") Long id);
}
