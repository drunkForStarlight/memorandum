package com.memoassistant.common;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import com.memoassistant.settings.MailSettings;
import com.memoassistant.settings.MailSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

@Service
public class MailService {
    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final MailSettingsService settingsService;

    public MailService(MailSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public boolean configured() {
        MailSettings settings = settingsService.get();
        return settings.host() != null && !settings.host().isBlank();
    }

    public boolean sendText(List<String> recipients, String subject, String content) {
        MailSettings settings = settingsService.get();
        List<String> cleanRecipients = recipients.stream()
                .filter(address -> address != null && !address.isBlank())
                .map(String::trim)
                .toList();
        if (cleanRecipients.isEmpty()) {
            log.info("Skip email '{}': no recipients configured", subject);
            return false;
        }
        if (settings.host() == null || settings.host().isBlank()) {
            log.info("Mail not configured. Would send '{}' to {}:\n{}", subject, cleanRecipients, content);
            return false;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(settings.from() == null || settings.from().isBlank() ? settings.username() : settings.from());
        message.setTo(cleanRecipients.toArray(String[]::new));
        message.setSubject(subject);
        message.setText(content);
        try {
            mailSender(settings).send(message);
            return true;
        } catch (MailException exception) {
            log.error("Failed to send email '{}'", subject, exception);
            return false;
        }
    }

    public static List<String> splitRecipients(String recipients) {
        if (recipients == null || recipients.isBlank()) {
            return List.of();
        }
        return Arrays.stream(recipients.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }

    private JavaMailSenderImpl mailSender(MailSettings settings) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(settings.host());
        sender.setPort(settings.port());
        sender.setUsername(settings.username());
        sender.setPassword(settings.password());
        sender.setDefaultEncoding("UTF-8");

        Properties properties = sender.getJavaMailProperties();
        properties.put("mail.smtp.auth", String.valueOf(settings.auth()));
        properties.put("mail.smtp.starttls.enable", String.valueOf(settings.starttls()));
        properties.put("mail.smtp.ssl.enable", String.valueOf(settings.port() == 465 && !settings.starttls()));
        properties.put("mail.smtp.connectiontimeout", "10000");
        properties.put("mail.smtp.timeout", "10000");
        properties.put("mail.smtp.writetimeout", "10000");
        return sender;
    }
}

