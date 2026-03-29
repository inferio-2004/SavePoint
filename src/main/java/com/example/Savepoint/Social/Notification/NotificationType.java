package com.example.Savepoint.Social.Notification;

public enum NotificationType {
    FOLLOW,      // someone followed you        → referenceType=USER,   referenceId=followerId
    LIKE,        // someone liked your review   → referenceType=REVIEW, referenceId=reviewId
    NEW_REVIEW   // someone you follow reviewed → referenceType=REVIEW, referenceId=reviewId
}
