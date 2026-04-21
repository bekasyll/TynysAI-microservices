package com.tynysai.notificationservice.controller;

import com.tynysai.notificationservice.dto.ApiResponse;
import com.tynysai.notificationservice.dto.PageResponse;
import com.tynysai.notificationservice.dto.request.CreateNotificationRequest;
import com.tynysai.notificationservice.dto.response.NotificationResponse;
import com.tynysai.notificationservice.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<PageResponse<NotificationResponse>> list(@RequestHeader("X-User-Id") UUID userId, Pageable pageable) {
        return ApiResponse.success(notificationService.getNotifications(userId, pageable));
    }

    @GetMapping("/unread")
    public ApiResponse<PageResponse<NotificationResponse>> listUnread(@RequestHeader("X-User-Id") UUID userId,
                                                                      Pageable pageable) {
        return ApiResponse.success(notificationService.getUnreadNotifications(userId, pageable));
    }

    @GetMapping("/unread/count")
    public ApiResponse<Long> countUnread(@RequestHeader("X-User-Id") UUID userId) {
        return ApiResponse.success(notificationService.countUnread(userId));
    }

    /**
     * Internal endpoint used by other microservices to push a notification for a user.
     */
    @PostMapping
    public ApiResponse<NotificationResponse> create(@Valid @RequestBody CreateNotificationRequest request) {
        return ApiResponse.success("Notification created", notificationService.create(request));
    }

    @PostMapping("/{id}/read")
    public ApiResponse<Void> markAsRead(@PathVariable Long id, @RequestHeader("X-User-Id") UUID userId) {
        notificationService.markAsRead(id, userId);
        return ApiResponse.success("Marked as read", null);
    }

    @PostMapping("/read-all")
    public ApiResponse<Integer> markAllAsRead(@RequestHeader("X-User-Id") UUID userId) {
        return ApiResponse.success("Marked all as read", notificationService.markAllAsRead(userId));
    }

    @DeleteMapping
    public ApiResponse<Void> deleteAll(@RequestHeader("X-User-Id") UUID userId) {
        notificationService.deleteAllForUser(userId);
        return ApiResponse.success("Deleted", null);
    }
}
