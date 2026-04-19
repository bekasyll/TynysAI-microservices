package com.tynysai.notificationservice.dto.request;

import com.tynysai.notificationservice.model.enums.NotificationType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class CreateNotificationRequest {
    @NotNull
    private Long userId;

    @NotNull
    private NotificationType type;
    private Map<String, String> params;
    private String relatedEntityId;
    private String relatedEntityType;
}
