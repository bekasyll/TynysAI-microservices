package com.tynysai.xrayservice.client;

import com.tynysai.common.client.dto.UserDto;
import com.tynysai.common.dto.ApiResponse;
import com.tynysai.xrayservice.client.fallback.UserFeignClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "user-service", fallback = UserFeignClientFallback.class)
public interface UserFeignClient {
    @GetMapping("/api/users/{id}")
    ApiResponse<UserDto> getUserById(@PathVariable("id") UUID id);

    @GetMapping("/api/users/search-ids")
    ApiResponse<List<UUID>> searchUserIds(@RequestParam("role") String role,
                                          @RequestParam("q") String q);
}
