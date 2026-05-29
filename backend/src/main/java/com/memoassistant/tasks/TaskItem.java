package com.memoassistant.tasks;

import java.time.LocalDateTime;

public record TaskItem(
        Long id,
        String title,
        String description,
        String category,
        TaskPriority priority,
        TaskStatus status,
        LocalDateTime dueAt,
        LocalDateTime remindAt,
        LocalDateTime reminderSentAt,
        LocalDateTime completedAt,
        Long createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}

