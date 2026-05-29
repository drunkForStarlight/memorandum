package com.memoassistant.settings;

public record MailSettings(
        String host,
        int port,
        String username,
        String password,
        boolean auth,
        boolean starttls,
        String from,
        String reminderRecipients,
        String weeklyRecipients,
        boolean weeklyEnabled,
        int weeklyDay,
        String weeklyTime) {
}

