package com.tynysai.notificationservice.dto.response;

import com.tynysai.notificationservice.model.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private Long id;
    private UUID userId;

    /**
     * Notification code — client looks up the localized title/message by this key.
     */
    private NotificationType type;

    /**
     * Placeholder values (e.g. {"doctorName": "Ivanov", "testType": "CBC"}).
     * Client substitutes these into the localized template.
     */
    private Map<String, String> params;

    private boolean read;
    private String relatedEntityId;
    private String relatedEntityType;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
}
