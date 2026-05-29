package com.memoassistant.settings;

public final class SettingsDtos {
    private SettingsDtos() {
    }

    public record MailSettingsResponse(
            String host,
            int port,
            String username,
            boolean passwordSet,
            boolean auth,
            boolean starttls,
            String from,
            String reminderRecipients,
            String weeklyRecipients,
            boolean weeklyEnabled,
            int weeklyDay,
            String weeklyTime) {
    }

    public record MailSettingsRequest(
            String host,
            Integer port,
            String username,
            String password,
            Boolean clearPassword,
            Boolean auth,
            Boolean starttls,
            String from,
            String reminderRecipients,
            String weeklyRecipients,
            Boolean weeklyEnabled,
            Integer weeklyDay,
            String weeklyTime) {
    }

    public record TestMailRequest(String recipient) {
    }

    public record TestMailResponse(boolean ok, String message) {
    }
}

