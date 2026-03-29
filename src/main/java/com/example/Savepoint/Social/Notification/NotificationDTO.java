package com.example.Savepoint.Social.Notification;

import java.time.LocalDateTime;

public record NotificationDTO(
        Long id,
        NotificationType type,
        Long referenceId,
        ReferenceType referenceType,
        boolean isRead,
        LocalDateTime createdAt
) {}
