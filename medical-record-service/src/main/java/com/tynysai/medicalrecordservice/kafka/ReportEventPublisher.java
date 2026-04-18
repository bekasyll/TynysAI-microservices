package com.tynysai.medicalrecordservice.kafka;

import com.tynysai.medicalrecordservice.events.ReportCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReportEventPublisher {
    private static final String BINDING = "sendReportCreated-out-0";
    private final StreamBridge streamBridge;

    public void publishReportCreated(ReportCreatedEvent event) {
        streamBridge.send(BINDING, event);
    }
}
