package com.tynysai.medicalrecordservice.client;

import com.tynysai.medicalrecordservice.client.dto.DoctorDto;
import com.tynysai.medicalrecordservice.client.dto.UserDto;
import com.tynysai.medicalrecordservice.client.dto.WrappedResponse;
import com.tynysai.medicalrecordservice.client.fallback.UserFeignClientFallback;
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
