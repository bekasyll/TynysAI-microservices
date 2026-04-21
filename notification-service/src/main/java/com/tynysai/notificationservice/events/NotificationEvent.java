package com.tynysai.notificationservice.events;

import com.tynysai.notificationservice.model.enums.NotificationType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class NotificationEvent {
    private UUID userId;

    /**
     * Notification code (name of {@link NotificationType}).
     */
    private String type;
    private Map<String, String> params;
    private String relatedEntityId;
    private String relatedEntityType;
}
