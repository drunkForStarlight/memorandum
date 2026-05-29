package com.memoassistant.reports;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record WeeklyReport(
        Long id,
        LocalDate weekStart,
        LocalDate weekEnd,
        String subject,
        String content,
        String sentTo,
        LocalDateTime sentAt,
        LocalDateTime createdAt) {
}

