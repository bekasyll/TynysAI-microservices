package com.tynysai.xrayservice.client;

import com.tynysai.xrayservice.client.dto.UserDto;
import com.tynysai.xrayservice.client.dto.WrappedResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", url = "${services.user-service.url}")
public interface UserFeignClient {
    @GetMapping("/api/users/{id}")
    WrappedResponse<UserDto> getUserById(@PathVariable("id") Long id);
}
