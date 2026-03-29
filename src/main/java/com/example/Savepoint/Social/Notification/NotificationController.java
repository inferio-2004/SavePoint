package com.example.Savepoint.Social.Notification;

import com.example.Savepoint.Auth.SessionAuthPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<Page<NotificationDTO>> getNotifications(
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication) {
        Integer userId = getPrincipal(authentication).id();
        return ResponseEntity.ok(notificationService.getNotifications(userId, pageable));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long notificationId,
            Authentication authentication) {
        Integer userId = getPrincipal(authentication).id();
        notificationService.markAsRead(notificationId, userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(Authentication authentication) {
        Integer userId = getPrincipal(authentication).id();
        notificationService.markAllAsRead(userId);
        return ResponseEntity.noContent().build();
    }

    private SessionAuthPrincipal getPrincipal(Authentication authentication) {
        return (SessionAuthPrincipal) authentication.getPrincipal();
    }
}
