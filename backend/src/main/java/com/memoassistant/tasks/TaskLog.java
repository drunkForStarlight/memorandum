package com.memoassistant.tasks;

import java.time.LocalDateTime;

public record TaskLog(
        Long id,
        Long taskId,
        String content,
        Long createdBy,
        LocalDateTime createdAt) {
}

