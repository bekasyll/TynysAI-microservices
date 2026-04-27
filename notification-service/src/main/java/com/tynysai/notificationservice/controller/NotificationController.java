package com.tynysai.notificationservice.controller;

import com.tynysai.common.dto.ApiResponse;
import com.tynysai.common.dto.PageResponse;
import com.tynysai.notificationservice.dto.request.CreateNotificationRequest;
import com.tynysai.notificationservice.dto.response.NotificationResponse;
import com.tynysai.common.security.CurrentUserId;
import com.tynysai.notificationservice.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Уведомления пользователю (i18n-коды + JSON-параметры для рендера)")
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "Все уведомления пользователя")
    public ApiResponse<PageResponse<NotificationResponse>> list(@CurrentUserId UUID userId, Pageable pageable) {
        return ApiResponse.success(notificationService.getNotifications(userId, pageable));
    }

    @GetMapping("/unread")
    @Operation(summary = "Непрочитанные уведомления")
    public ApiResponse<PageResponse<NotificationResponse>> listUnread(@CurrentUserId UUID userId,
                                                                      Pageable pageable) {
        return ApiResponse.success(notificationService.getUnreadNotifications(userId, pageable));
    }

    @GetMapping("/unread/count")
    @Operation(summary = "Количество непрочитанных уведомлений")
    public ApiResponse<Long> countUnread(@CurrentUserId UUID userId) {
        return ApiResponse.success(notificationService.countUnread(userId));
    }

    @PostMapping
    @Operation(summary = "Создать уведомление",
            description = "Internal endpoint - вызывается другими микросервисами при необходимости push-уведомления")
    public ApiResponse<NotificationResponse> create(@Valid @RequestBody CreateNotificationRequest request) {
        return ApiResponse.success("Notification created", notificationService.create(request));
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "Пометить уведомление прочитанным")
    public ApiResponse<Void> markAsRead(@PathVariable Long id, @CurrentUserId UUID userId) {
        notificationService.markAsRead(id, userId);
        return ApiResponse.success("Marked as read", null);
    }

    @PostMapping("/read-all")
    @Operation(summary = "Пометить все прочитанными")
    public ApiResponse<Integer> markAllAsRead(@CurrentUserId UUID userId) {
        return ApiResponse.success("Marked all as read", notificationService.markAllAsRead(userId));
    }

    @DeleteMapping
    @Operation(summary = "Удалить все уведомления пользователя")
    public ApiResponse<Void> deleteAll(@CurrentUserId UUID userId) {
        notificationService.deleteAllForUser(userId);
        return ApiResponse.success("Deleted", null);
    }
}
