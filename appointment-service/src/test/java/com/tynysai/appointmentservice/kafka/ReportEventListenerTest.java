package com.tynysai.appointmentservice.kafka;

import com.tynysai.appointmentservice.events.ReportCreatedEvent;
import com.tynysai.appointmentservice.service.AppointmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReportEventListenerTest {
    @Mock
    private AppointmentService appointmentService;

    private Consumer<ReportCreatedEvent> consumer;

    @BeforeEach
    void setUp() {
        consumer = new ReportEventListener().handleReportCreated(appointmentService);
    }

    @Test
    void handleReportCreated_invokesLinkReport_whenAllFieldsPresent() {
        UUID doctorId = UUID.randomUUID();
        ReportCreatedEvent event = ReportCreatedEvent.builder()
                .reportId(555L)
                .appointmentId(10L)
                .doctorId(doctorId)
                .patientId(UUID.randomUUID())
                .build();

        consumer.accept(event);

        verify(appointmentService).linkReport(10L, doctorId, 555L);
    }

    @Test
    void handleReportCreated_skips_whenAppointmentIdIsNull() {
        ReportCreatedEvent event = ReportCreatedEvent.builder()
                .reportId(555L)
                .doctorId(UUID.randomUUID())
                .build();

        consumer.accept(event);

        verify(appointmentService, never()).linkReport(any(), any(), anyLong());
    }

    @Test
    void handleReportCreated_skips_whenDoctorIdIsNull() {
        ReportCreatedEvent event = ReportCreatedEvent.builder()
                .reportId(555L)
                .appointmentId(10L)
                .build();

        consumer.accept(event);

        verify(appointmentService, never()).linkReport(any(), any(), anyLong());
    }

    @Test
    void handleReportCreated_skips_whenReportIdIsNull() {
        ReportCreatedEvent event = ReportCreatedEvent.builder()
                .appointmentId(10L)
                .doctorId(UUID.randomUUID())
                .build();

        consumer.accept(event);

        verify(appointmentService, never()).linkReport(any(), any(), anyLong());
    }

    @Test
    void handleReportCreated_swallowsExceptions_thrownByService() {
        UUID doctorId = UUID.randomUUID();
        ReportCreatedEvent event = ReportCreatedEvent.builder()
                .reportId(555L)
                .appointmentId(10L)
                .doctorId(doctorId)
                .build();
        doThrow(new RuntimeException("db down")).when(appointmentService).linkReport(10L, doctorId, 555L);

        consumer.accept(event);

        verify(appointmentService).linkReport(10L, doctorId, 555L);
    }
}