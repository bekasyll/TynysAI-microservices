package com.tynysai.notificationservice.kafka;

import com.tynysai.notificationservice.dto.request.CreateNotificationRequest;
import com.tynysai.notificationservice.events.NotificationEvent;
import com.tynysai.notificationservice.model.enums.NotificationType;
import com.tynysai.notificationservice.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
@Slf4j
public class NotificationEventListener {

    @Bean
    public Consumer<NotificationEvent> handleNotification(NotificationService notificationService) {
        return event -> {
            try {
                if (event.getUserId() == null || event.getType() == null) {
                    log.warn("Skipping notification event with missing fields: {}", event);
                    return;
                }

                NotificationType type;
                try {
                    type = NotificationType.valueOf(event.getType());
                } catch (IllegalArgumentException e) {
                    log.warn("Unknown notification type '{}', skipping event", event.getType());
                    return;
                }

                CreateNotificationRequest request = new CreateNotificationRequest();
                request.setUserId(event.getUserId());
                request.setType(type);
                request.setParams(event.getParams());
                request.setRelatedEntityId(event.getRelatedEntityId());
                request.setRelatedEntityType(event.getRelatedEntityType());

                notificationService.create(request);
            } catch (Exception e) {
                log.error("Failed to process notification event '{}': {}", event, e.getMessage(), e);
            }
        };
    }
}
