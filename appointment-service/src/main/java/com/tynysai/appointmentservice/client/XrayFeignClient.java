package com.tynysai.appointmentservice.client;

import com.tynysai.appointmentservice.client.fallback.XrayFeignClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "xray-service", fallback = XrayFeignClientFallback.class)
public interface XrayFeignClient {
    @GetMapping("/api/xrays/{id}")
    Object getXrayForPatient(@PathVariable("id") Long id, @RequestParam("patientId") UUID patientId);
}
