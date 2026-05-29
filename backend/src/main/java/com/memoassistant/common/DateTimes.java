package com.memoassistant.common;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class DateTimes {
    private DateTimes() {
    }

    public static String text(LocalDateTime value) {
        return value == null ? null : value.toString();
    }

    public static String text(LocalDate value) {
        return value == null ? null : value.toString();
    }

    public static LocalDateTime parseDateTime(String value) {
        return value == null || value.isBlank() ? null : LocalDateTime.parse(value);
    }

    public static LocalDate parseDate(String value) {
        return value == null || value.isBlank() ? null : LocalDate.parse(value);
    }
}

