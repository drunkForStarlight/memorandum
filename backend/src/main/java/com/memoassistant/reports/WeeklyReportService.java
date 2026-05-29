package com.memoassistant.reports;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.memoassistant.common.MailService;
import com.memoassistant.config.AppLifecycle;
import com.memoassistant.settings.MailSettings;
import com.memoassistant.settings.MailSettingsService;
import com.memoassistant.tasks.TaskItem;
import com.memoassistant.tasks.TaskRepository;
import com.memoassistant.tasks.TaskStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class WeeklyReportService {
    private final WeeklyReportRepository reportRepository;
    private final TaskRepository taskRepository;
    private final MailService mailService;
    private final AppLifecycle appLifecycle;
    private final MailSettingsService settingsService;

    public WeeklyReportService(WeeklyReportRepository reportRepository,
                               TaskRepository taskRepository,
                               MailService mailService,
                               AppLifecycle appLifecycle,
                               MailSettingsService settingsService) {
        this.reportRepository = reportRepository;
        this.taskRepository = taskRepository;
        this.mailService = mailService;
        this.appLifecycle = appLifecycle;
        this.settingsService = settingsService;
    }

    public List<WeeklyReport> list() {
        return reportRepository.findAll();
    }

    public WeeklyReport generateCurrentWeek() {
        LocalDate today = LocalDate.now();
        LocalDate start = today.with(DayOfWeek.MONDAY);
        LocalDate end = start.plusDays(6);
        return generate(start, end);
    }

    public WeeklyReport generate(LocalDate start, LocalDate end) {
        List<TaskItem> tasks = taskRepository.findAll();
        List<TaskItem> completed = tasks.stream()
                .filter(task -> task.status() == TaskStatus.DONE)
                .filter(task -> task.completedAt() != null)
                .filter(task -> !task.completedAt().toLocalDate().isBefore(start)
                        && !task.completedAt().toLocalDate().isAfter(end))
                .toList();
        List<TaskItem> overdue = tasks.stream()
                .filter(task -> task.status() != TaskStatus.DONE && task.status() != TaskStatus.CANCELED)
                .filter(task -> task.dueAt() != null && task.dueAt().toLocalDate().isBefore(LocalDate.now()))
                .toList();
        Map<String, Long> byCategory = completed.stream()
                .collect(Collectors.groupingBy(task -> blankToDefault(task.category(), "未分类"), Collectors.counting()));

        String subject = "个人备忘周报 " + start + " 至 " + end;
        StringBuilder content = new StringBuilder();
        content.append(subject).append("\n\n");
        content.append("本周完成：").append(completed.size()).append(" 项\n");
        content.append("当前逾期：").append(overdue.size()).append(" 项\n\n");

        content.append("完成事项\n");
        if (completed.isEmpty()) {
            content.append("- 暂无完成记录\n");
        } else {
            completed.forEach(task -> content.append("- [")
                    .append(blankToDefault(task.category(), "未分类"))
                    .append("] ")
                    .append(task.title())
                    .append("（")
                    .append(task.priority())
                    .append("）\n"));
        }

        content.append("\n分类统计\n");
        if (byCategory.isEmpty()) {
            content.append("- 暂无\n");
        } else {
            byCategory.forEach((category, count) -> content.append("- ")
                    .append(category)
                    .append("：")
                    .append(count)
                    .append("\n"));
        }

        content.append("\n待关注逾期事项\n");
        if (overdue.isEmpty()) {
            content.append("- 暂无\n");
        } else {
            overdue.stream().limit(10).forEach(task -> content.append("- ")
                    .append(task.title())
                    .append("，截止：")
                    .append(task.dueAt())
                    .append("\n"));
        }

        return reportRepository.create(start, end, subject, content.toString());
    }

    public WeeklyReport send(Long id) {
        WeeklyReport report = reportRepository.findById(id);
        List<String> recipients = MailService.splitRecipients(settingsService.get().weeklyRecipients());
        if (mailService.sendText(recipients, report.subject(), report.content())) {
            return reportRepository.markSent(id, String.join(",", recipients));
        }
        return report;
    }

    @Scheduled(fixedDelay = 60_000)
    public void generateAndSendScheduledReport() {
        if (!appLifecycle.ready()) {
            return;
        }
        MailSettings settings = settingsService.get();
        if (!settings.weeklyEnabled()) {
            return;
        }
        List<String> recipients = MailService.splitRecipients(settings.weeklyRecipients());
        if (!recipients.isEmpty()) {
            LocalDate today = LocalDate.now();
            LocalDateTime now = LocalDateTime.now();
            String currentTime = now.format(DateTimeFormatter.ofPattern("HH:mm"));
            if (now.getDayOfWeek().getValue() == settings.weeklyDay()
                    && currentTime.equals(settings.weeklyTime())
                    && !today.toString().equals(settingsService.getWeeklyLastSentDate())) {
                WeeklyReport report = generateCurrentWeek();
                if (mailService.sendText(recipients, report.subject(), report.content())) {
                    reportRepository.markSent(report.id(), String.join(",", recipients));
                    settingsService.setWeeklyLastSentDate(today.toString());
                }
            }
        }
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
