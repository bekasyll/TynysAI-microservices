package com.tynysai.xrayservice.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {
    private Long userId;
    private String type;
    private Map<String, String> params;
    private String relatedEntityId;
    private String relatedEntityType;
}
