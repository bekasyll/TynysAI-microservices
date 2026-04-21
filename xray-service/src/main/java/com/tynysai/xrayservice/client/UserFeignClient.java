package com.tynysai.xrayservice.client;

import com.tynysai.xrayservice.client.dto.UserDto;
import com.tynysai.xrayservice.client.dto.WrappedResponse;
import com.tynysai.xrayservice.client.fallback.UserFeignClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", url = "${services.user-service.url}", fallback = UserFeignClientFallback.class)
public interface UserFeignClient {
    @GetMapping("/api/users/{id}")
    WrappedResponse<UserDto> getUserById(@PathVariable("id") Long id);
}
