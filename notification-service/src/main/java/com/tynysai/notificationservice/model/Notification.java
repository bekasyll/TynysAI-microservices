package com.tynysai.notificationservice.model;

import com.tynysai.notificationservice.model.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notification_user", columnList = "user_id"),
        @Index(name = "idx_notification_user_read", columnList = "user_id, read")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false, nullable = false)
    private Long id;

    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    private UUID userId;

    /**
     * Notification code. The client looks up its i18n dictionary by this code
     * to render the localized title and message.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private NotificationType type;

    /**
     * JSON-serialized Map<String, String> with placeholders (e.g. {"doctorName": "Ivanov"}).
     * The client substitutes these into the localized template.
     */
    @Column(name = "params", columnDefinition = "TEXT")
    private String paramsJson;

    @Column(nullable = false)
    @Builder.Default
    private boolean read = false;

    @Column(length = 64)
    private String relatedEntityId;

    @Column(length = 50)
    private String relatedEntityType;

    private LocalDateTime readAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
