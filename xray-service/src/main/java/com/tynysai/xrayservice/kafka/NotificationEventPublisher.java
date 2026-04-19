package com.tynysai.xrayservice.kafka;

import com.tynysai.xrayservice.events.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventPublisher {
    private static final String BINDING = "sendNotification-out-0";
    private final StreamBridge streamBridge;

    public void publish(Long userId, String type, String relatedEntityId,
                        String relatedEntityType, Map<String, String> params) {
        NotificationEvent event = NotificationEvent.builder()
                .userId(userId)
                .type(type)
                .params(params)
                .relatedEntityId(relatedEntityId)
                .relatedEntityType(relatedEntityType)
                .build();
        streamBridge.send(BINDING, event);
    }
}
