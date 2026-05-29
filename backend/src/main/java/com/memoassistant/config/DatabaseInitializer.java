package com.memoassistant.config;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DatabaseInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminPassword;
    private final String adminDisplayName;
    private final String mailHost;
    private final String mailPort;
    private final String mailUsername;
    private final String mailPassword;
    private final String mailAuth;
    private final String mailStarttls;
    private final String mailFrom;
    private final String weeklyRecipients;

    public DatabaseInitializer(
            DataSource dataSource,
            JdbcTemplate jdbcTemplate,
            PasswordEncoder passwordEncoder,
            @Value("${memo.admin.username}") String adminUsername,
            @Value("${memo.admin.password}") String adminPassword,
            @Value("${memo.admin.display-name}") String adminDisplayName,
            @Value("${spring.mail.host:}") String mailHost,
            @Value("${spring.mail.port:25}") String mailPort,
            @Value("${spring.mail.username:}") String mailUsername,
            @Value("${spring.mail.password:}") String mailPassword,
            @Value("${spring.mail.properties.mail.smtp.auth:false}") String mailAuth,
            @Value("${spring.mail.properties.mail.smtp.starttls.enable:false}") String mailStarttls,
            @Value("${memo.mail.from}") String mailFrom,
            @Value("${memo.weekly.recipients}") String weeklyRecipients) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        this.adminDisplayName = adminDisplayName;
        this.mailHost = mailHost;
        this.mailPort = mailPort;
        this.mailUsername = mailUsername;
        this.mailPassword = mailPassword;
        this.mailAuth = mailAuth;
        this.mailStarttls = mailStarttls;
        this.mailFrom = mailFrom;
        this.weeklyRecipients = weeklyRecipients;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        jdbcTemplate.execute("PRAGMA journal_mode=WAL");
        jdbcTemplate.execute("PRAGMA foreign_keys=ON");
        createTables();
        seedSettings();
        createDefaultAdmin();
    }

    private void createTables() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS app_users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT NOT NULL UNIQUE,
                    password_hash TEXT NOT NULL,
                    display_name TEXT NOT NULL,
                    role TEXT NOT NULL DEFAULT 'USER',
                    enabled INTEGER NOT NULL DEFAULT 1,
                    created_at TEXT NOT NULL,
                    updated_at TEXT NOT NULL
                )
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS tasks (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT NOT NULL,
                    description TEXT,
                    category TEXT,
                    priority TEXT NOT NULL DEFAULT 'MEDIUM',
                    status TEXT NOT NULL DEFAULT 'TODO',
                    due_at TEXT,
                    remind_at TEXT,
                    reminder_sent_at TEXT,
                    completed_at TEXT,
                    created_by INTEGER NOT NULL,
                    created_at TEXT NOT NULL,
                    updated_at TEXT NOT NULL,
                    FOREIGN KEY(created_by) REFERENCES app_users(id)
                )
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS task_logs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    task_id INTEGER NOT NULL,
                    content TEXT NOT NULL,
                    created_by INTEGER NOT NULL,
                    created_at TEXT NOT NULL,
                    FOREIGN KEY(task_id) REFERENCES tasks(id) ON DELETE CASCADE,
                    FOREIGN KEY(created_by) REFERENCES app_users(id)
                )
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS weekly_reports (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    week_start TEXT NOT NULL,
                    week_end TEXT NOT NULL,
                    subject TEXT NOT NULL,
                    content TEXT NOT NULL,
                    sent_to TEXT,
                    sent_at TEXT,
                    created_at TEXT NOT NULL
                )
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS system_settings (
                    setting_key TEXT PRIMARY KEY,
                    setting_value TEXT,
                    updated_at TEXT NOT NULL
                )
                """);
    }

    private void seedSettings() {
        putSettingIfAbsent("mail.smtp.host", mailHost);
        putSettingIfAbsent("mail.smtp.port", mailPort);
        putSettingIfAbsent("mail.smtp.username", mailUsername);
        putSettingIfAbsent("mail.smtp.password", mailPassword);
        putSettingIfAbsent("mail.smtp.auth", mailAuth);
        putSettingIfAbsent("mail.smtp.starttls", mailStarttls);
        putSettingIfAbsent("mail.from", mailFrom);
        putSettingIfAbsent("mail.reminder.recipients", weeklyRecipients);
        putSettingIfAbsent("mail.weekly.recipients", weeklyRecipients);
        putSettingIfAbsent("weekly.enabled", "true");
        putSettingIfAbsent("weekly.day", "5");
        putSettingIfAbsent("weekly.time", "18:00");
        putSettingIfAbsent("weekly.last_sent_date", "");
    }

    private void putSettingIfAbsent(String key, String value) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM system_settings WHERE setting_key = ?",
                Integer.class,
                key);
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.update("""
                INSERT INTO system_settings(setting_key, setting_value, updated_at)
                VALUES (?, ?, ?)
                """, key, value == null ? "" : value, java.time.LocalDateTime.now().toString());
    }

    private void createDefaultAdmin() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM app_users", Integer.class);
        if (count != null && count > 0) {
            return;
        }
        String now = java.time.LocalDateTime.now().toString();
        jdbcTemplate.update("""
                INSERT INTO app_users(username, password_hash, display_name, role, enabled, created_at, updated_at)
                VALUES (?, ?, ?, 'ADMIN', 1, ?, ?)
                """, adminUsername, passwordEncoder.encode(adminPassword), adminDisplayName, now, now);
        log.warn("Created default admin user '{}'. Change the password before production use.", adminUsername);
    }
}
