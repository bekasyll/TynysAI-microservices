package com.tynysai.appointmentservice.client.fallback;

import com.tynysai.appointmentservice.client.XrayFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class XrayFeignClientFallback implements XrayFeignClient {
    @Override
    public Object getXrayForPatient(Long id, Long patientId) {
        log.warn("XrayFeignClient fallback triggered for getXrayForPatient(id={}, patientId={})", id, patientId);
        return null;
    }
}