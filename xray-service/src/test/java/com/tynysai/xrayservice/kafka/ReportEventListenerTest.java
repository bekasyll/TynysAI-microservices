package com.tynysai.xrayservice.kafka;

import com.tynysai.xrayservice.events.ReportCreatedEvent;
import com.tynysai.xrayservice.service.XrayAnalysisService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReportEventListenerTest {
    @Mock
    private XrayAnalysisService xrayAnalysisService;

    private final ReportEventListener listener = new ReportEventListener();

    @Test
    void handleReportCreated_callsMarkCompleted_whenXrayAnalysisIdPresent() {
        Consumer<ReportCreatedEvent> consumer = listener.handleReportCreated(xrayAnalysisService);
        ReportCreatedEvent event = ReportCreatedEvent.builder()
                .reportId(10L)
                .patientId(UUID.randomUUID())
                .doctorId(UUID.randomUUID())
                .appointmentId(5L)
                .xrayAnalysisId(42L)
                .build();

        consumer.accept(event);

        verify(xrayAnalysisService).markCompleted(42L);
    }

    @Test
    void handleReportCreated_skips_whenXrayAnalysisIdIsNull() {
        Consumer<ReportCreatedEvent> consumer = listener.handleReportCreated(xrayAnalysisService);
        ReportCreatedEvent event = ReportCreatedEvent.builder()
                .reportId(10L)
                .xrayAnalysisId(null)
                .build();

        consumer.accept(event);

        verify(xrayAnalysisService, never()).markCompleted(anyLong());
    }

    @Test
    void handleReportCreated_swallowsExceptions_thrownByService() {
        Consumer<ReportCreatedEvent> consumer = listener.handleReportCreated(xrayAnalysisService);
        ReportCreatedEvent event = ReportCreatedEvent.builder()
                .reportId(11L)
                .xrayAnalysisId(99L)
                .build();
        doThrow(new RuntimeException("downstream failure"))
                .when(xrayAnalysisService).markCompleted(99L);

        consumer.accept(event);

        verify(xrayAnalysisService).markCompleted(99L);
    }
}