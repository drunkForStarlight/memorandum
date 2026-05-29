package com.memoassistant.settings;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class SettingsRepository {
    private final JdbcClient jdbc;

    public SettingsRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<String> get(String key) {
        return jdbc.sql("SELECT setting_value FROM system_settings WHERE setting_key = ?")
                .param(key)
                .query(String.class)
                .optional();
    }

    public Map<String, String> all() {
        return jdbc.sql("SELECT setting_key, setting_value FROM system_settings")
                .query((rs, rowNum) -> Map.entry(rs.getString("setting_key"), rs.getString("setting_value")))
                .list()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public void put(String key, String value) {
        String now = LocalDateTime.now().toString();
        int updated = jdbc.sql("""
                UPDATE system_settings
                SET setting_value = ?, updated_at = ?
                WHERE setting_key = ?
                """)
                .params(value == null ? "" : value, now, key)
                .update();
        if (updated == 0) {
            jdbc.sql("""
                    INSERT INTO system_settings(setting_key, setting_value, updated_at)
                    VALUES (?, ?, ?)
                    """)
                    .params(key, value == null ? "" : value, now)
                    .update();
        }
    }
}

