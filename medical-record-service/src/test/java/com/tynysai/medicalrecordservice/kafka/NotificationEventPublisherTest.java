package com.tynysai.medicalrecordservice.kafka;

import com.tynysai.medicalrecordservice.events.NotificationEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.stream.function.StreamBridge;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationEventPublisherTest {
    @Mock
    private StreamBridge streamBridge;

    @InjectMocks
    private NotificationEventPublisher publisher;

    @Test
    void publish_sendsBuiltNotificationEvent_toBinding() {
        UUID userId = UUID.randomUUID();
        Map<String, String> params = Map.of("k", "v");

        publisher.publish(userId, "REPORT_READY", "42", "DiagnosticReport", params);

        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(streamBridge).send(eq("sendNotification-out-0"), captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(userId);
        assertThat(captor.getValue().getType()).isEqualTo("REPORT_READY");
        assertThat(captor.getValue().getRelatedEntityId()).isEqualTo("42");
        assertThat(captor.getValue().getRelatedEntityType()).isEqualTo("DiagnosticReport");
        assertThat(captor.getValue().getParams()).isEqualTo(params);
    }
}
