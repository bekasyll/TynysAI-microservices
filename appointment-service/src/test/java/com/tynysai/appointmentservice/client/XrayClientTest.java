package com.tynysai.appointmentservice.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class XrayClientTest {
    @Mock
    private XrayFeignClient feign;

    @InjectMocks
    private XrayClient client;

    private UUID patientId;

    @BeforeEach
    void setUp() {
        patientId = UUID.randomUUID();
    }

    @Test
    void existsForPatient_returnsTrue_whenFeignSucceeds() {
        when(feign.getXrayForPatient(42L, patientId)).thenReturn(new Object());

        boolean result = client.existsForPatient(42L, patientId);

        assertThat(result).isTrue();
        verify(feign).getXrayForPatient(42L, patientId);
    }

    @Test
    void existsForPatient_returnsTrue_whenFeignReturnsNull() {
        // The XrayClient implementation only catches exceptions, so a successful call (even with null)
        // is treated as "exists" - the fallback uses an exception path or returns null without throwing.
        when(feign.getXrayForPatient(42L, patientId)).thenReturn(null);

        boolean result = client.existsForPatient(42L, patientId);

        assertThat(result).isTrue();
    }

    @Test
    void existsForPatient_returnsFalse_whenFeignThrows() {
        when(feign.getXrayForPatient(42L, patientId)).thenThrow(new RuntimeException("404 Not Found"));

        boolean result = client.existsForPatient(42L, patientId);

        assertThat(result).isFalse();
    }
}
