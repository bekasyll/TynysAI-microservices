package com.tynysai.xrayservice.kafka;

import com.tynysai.xrayservice.events.ReportCreatedEvent;
import com.tynysai.xrayservice.service.XrayAnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
@Slf4j
public class ReportEventListener {

    @Bean
    public Consumer<ReportCreatedEvent> handleReportCreated(XrayAnalysisService xrayAnalysisService) {
        return event -> {
            try {
                if (event.getXrayAnalysisId() == null) {
                    log.debug("ReportCreatedEvent without xrayAnalysisId — skip");
                    return;
                }
                xrayAnalysisService.markCompleted(event.getXrayAnalysisId());
            } catch (Exception e) {
                log.error("Failed to process ReportCreatedEvent '{}': {}", event, e.getMessage(), e);
            }
        };
    }
}
