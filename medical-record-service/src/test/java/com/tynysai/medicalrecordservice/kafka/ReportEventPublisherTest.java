package com.tynysai.medicalrecordservice.kafka;

import com.tynysai.medicalrecordservice.events.ReportCreatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.stream.function.StreamBridge;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReportEventPublisherTest {
    @Mock
    private StreamBridge streamBridge;

    @InjectMocks
    private ReportEventPublisher publisher;

    @Test
    void publishReportCreated_sendsEventToCorrectBinding() {
        ReportCreatedEvent event = ReportCreatedEvent.builder()
                .reportId(1L)
                .patientId(UUID.randomUUID())
                .doctorId(UUID.randomUUID())
                .build();

        publisher.publishReportCreated(event);

        verify(streamBridge).send("sendReportCreated-out-0", event);
    }
}