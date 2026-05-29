package com.memoassistant.settings;

import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class MailSettingsService {
    private final SettingsRepository settingsRepository;

    public MailSettingsService(SettingsRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
    }

    public MailSettings get() {
        Map<String, String> values = settingsRepository.all();
        return new MailSettings(
                value(values, "mail.smtp.host"),
                intValue(values, "mail.smtp.port", 25),
                value(values, "mail.smtp.username"),
                value(values, "mail.smtp.password"),
                booleanValue(values, "mail.smtp.auth", false),
                booleanValue(values, "mail.smtp.starttls", false),
                value(values, "mail.from"),
                value(values, "mail.reminder.recipients"),
                value(values, "mail.weekly.recipients"),
                booleanValue(values, "weekly.enabled", true),
                intValue(values, "weekly.day", 5),
                value(values, "weekly.time", "18:00"));
    }

    public SettingsDtos.MailSettingsResponse publicSettings() {
        MailSettings settings = get();
        return new SettingsDtos.MailSettingsResponse(
                settings.host(),
                settings.port(),
                settings.username(),
                settings.password() != null && !settings.password().isBlank(),
                settings.auth(),
                settings.starttls(),
                settings.from(),
                settings.reminderRecipients(),
                settings.weeklyRecipients(),
                settings.weeklyEnabled(),
                settings.weeklyDay(),
                settings.weeklyTime());
    }

    public SettingsDtos.MailSettingsResponse update(SettingsDtos.MailSettingsRequest request) {
        put("mail.smtp.host", request.host());
        put("mail.smtp.port", String.valueOf(request.port() == null ? 25 : request.port()));
        put("mail.smtp.username", request.username());
        if (Boolean.TRUE.equals(request.clearPassword())) {
            put("mail.smtp.password", "");
        } else if (request.password() != null && !request.password().isBlank()) {
            put("mail.smtp.password", request.password());
        }
        put("mail.smtp.auth", String.valueOf(Boolean.TRUE.equals(request.auth())));
        put("mail.smtp.starttls", String.valueOf(Boolean.TRUE.equals(request.starttls())));
        put("mail.from", request.from());
        put("mail.reminder.recipients", request.reminderRecipients());
        put("mail.weekly.recipients", request.weeklyRecipients());
        put("weekly.enabled", String.valueOf(!Boolean.FALSE.equals(request.weeklyEnabled())));
        put("weekly.day", String.valueOf(clamp(request.weeklyDay() == null ? 5 : request.weeklyDay(), 1, 7)));
        put("weekly.time", validTime(request.weeklyTime()) ? request.weeklyTime() : "18:00");
        return publicSettings();
    }

    public String getWeeklyLastSentDate() {
        return settingsRepository.get("weekly.last_sent_date").orElse("");
    }

    public void setWeeklyLastSentDate(String date) {
        settingsRepository.put("weekly.last_sent_date", date);
    }

    private void put(String key, String value) {
        settingsRepository.put(key, value == null ? "" : value.trim());
    }

    private String value(Map<String, String> values, String key) {
        return value(values, key, "");
    }

    private String value(Map<String, String> values, String key, String fallback) {
        String value = values.get(key);
        return value == null || value.isBlank() ? fallback : value;
    }

    private int intValue(Map<String, String> values, String key, int fallback) {
        try {
            return Integer.parseInt(value(values, key, String.valueOf(fallback)));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private boolean booleanValue(Map<String, String> values, String key, boolean fallback) {
        String value = values.get(key);
        return value == null || value.isBlank() ? fallback : Boolean.parseBoolean(value);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private boolean validTime(String value) {
        return value != null && value.matches("^\\d{2}:\\d{2}$");
    }
}

