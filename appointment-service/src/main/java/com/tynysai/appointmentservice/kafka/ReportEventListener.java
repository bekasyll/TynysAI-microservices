package com.tynysai.appointmentservice.kafka;

import com.tynysai.appointmentservice.events.ReportCreatedEvent;
import com.tynysai.appointmentservice.service.AppointmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
@Slf4j
public class ReportEventListener {

    @Bean
    public Consumer<ReportCreatedEvent> handleReportCreated(AppointmentService appointmentService) {
        return event -> {
            try {
                if (event.getAppointmentId() == null || event.getDoctorId() == null || event.getReportId() == null) {
                    log.debug("ReportCreatedEvent without appointmentId — skip linking");
                    return;
                }
                appointmentService.linkReport(event.getAppointmentId(), event.getDoctorId(), event.getReportId());
            } catch (Exception e) {
                log.error("Failed to process ReportCreatedEvent '{}': {}", event, e.getMessage(), e);
            }
        };
    }
}
