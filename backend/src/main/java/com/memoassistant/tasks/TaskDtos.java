package com.memoassistant.tasks;

import java.time.LocalDateTime;
import java.util.List;

public final class TaskDtos {
    private TaskDtos() {
    }

    public record TaskRequest(
            String title,
            String description,
            String category,
            TaskPriority priority,
            TaskStatus status,
            LocalDateTime dueAt,
            LocalDateTime remindAt) {
    }

    public record LogRequest(String content) {
    }

    public record CompleteRequest(String summary) {
    }

    public record TaskDetail(TaskItem task, List<TaskLog> logs) {
    }
}

