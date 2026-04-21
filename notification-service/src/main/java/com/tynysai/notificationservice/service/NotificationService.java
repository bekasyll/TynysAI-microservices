package com.tynysai.notificationservice.service;

import com.tynysai.notificationservice.dto.PageResponse;
import com.tynysai.notificationservice.dto.request.CreateNotificationRequest;
import com.tynysai.notificationservice.dto.response.NotificationResponse;
import com.tynysai.notificationservice.exception.ResourceNotFoundException;
import com.tynysai.notificationservice.model.Notification;
import com.tynysai.notificationservice.repository.NotificationRepository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class NotificationService {
    private static final TypeReference<Map<String, String>> PARAMS_TYPE = new TypeReference<>() {};

    private final NotificationRepository repository;
    private final ObjectMapper objectMapper;

    public PageResponse<NotificationResponse> getNotifications(UUID userId, Pageable pageable) {
        Page<Notification> page = repository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return PageResponse.from(page.map(this::toResponse));
    }

    public PageResponse<NotificationResponse> getUnreadNotifications(UUID userId, Pageable pageable) {
        Page<Notification> page = repository.findByUserIdAndReadOrderByCreatedAtDesc(userId, false, pageable);
        return PageResponse.from(page.map(this::toResponse));
    }

    public long countUnread(UUID userId) {
        return repository.countByUserIdAndRead(userId, false);
    }

    @Transactional
    public NotificationResponse create(CreateNotificationRequest request) {
        Notification notification = Notification.builder()
                .userId(request.getUserId())
                .type(request.getType())
                .paramsJson(serializeParams(request.getParams()))
                .relatedEntityId(emptyToNull(request.getRelatedEntityId()))
                .relatedEntityType(emptyToNull(request.getRelatedEntityType()))
                .build();
        Notification saved = repository.save(notification);
        log.debug("Notification {} created for user {}", request.getType(), request.getUserId());
        return toResponse(saved);
    }

    @Transactional
    public void markAsRead(Long notificationId, UUID userId) {
        Notification n = repository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));
        if (!n.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Notification", "id", notificationId);
        }
        n.setRead(true);
        n.setReadAt(LocalDateTime.now());
        repository.save(n);
    }

    @Transactional
    public int markAllAsRead(UUID userId) {
        return repository.markAllAsRead(userId);
    }

    @Transactional
    public void deleteAllForUser(UUID userId) {
        repository.deleteByUserId(userId);
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .userId(n.getUserId())
                .type(n.getType())
                .params(deserializeParams(n.getParamsJson()))
                .read(n.isRead())
                .relatedEntityId(n.getRelatedEntityId())
                .relatedEntityType(n.getRelatedEntityType())
                .readAt(n.getReadAt())
                .createdAt(n.getCreatedAt())
                .build();
    }

    private String serializeParams(Map<String, String> params) {
        if (params == null || params.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(params);
        } catch (Exception e) {
            log.warn("Failed to serialize notification params: {}", e.getMessage());
            return null;
        }
    }

    private Map<String, String> deserializeParams(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(json, PARAMS_TYPE);
        } catch (Exception e) {
            log.warn("Failed to deserialize notification params '{}': {}", json, e.getMessage());
            return Map.of();
        }
    }

    private String emptyToNull(String v) {
        return v == null || v.isBlank() ? null : v;
    }
}
