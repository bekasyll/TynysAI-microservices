package com.tynysai.appointmentservice.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class XrayClient {
    private final XrayFeignClient xrayFeignClient;

    public boolean existsForPatient(Long xrayAnalysisId, UUID patientId) {
        try {
            xrayFeignClient.getXrayForPatient(xrayAnalysisId, patientId);
            return true;
        } catch (Exception e) {
            log.warn("X-ray {} not found for patient {}: {}", xrayAnalysisId, patientId, e.getMessage());
            return false;
        }
    }
}
