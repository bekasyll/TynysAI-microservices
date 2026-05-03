package com.tynysai.appointmentservice.client.fallback;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class XrayFeignClientFallbackTest {

    private final XrayFeignClientFallback fallback = new XrayFeignClientFallback();

    @Test
    void getXrayForPatient_returnsNull_whenInvoked() {
        Object result = fallback.getXrayForPatient(42L, UUID.randomUUID());

        assertThat(result).isNull();
    }
}
