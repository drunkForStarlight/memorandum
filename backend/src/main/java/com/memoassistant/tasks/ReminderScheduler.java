package com.memoassistant.tasks;

import java.time.LocalDateTime;
import java.util.List;

import com.memoassistant.common.MailService;
import com.memoassistant.config.AppLifecycle;
import com.memoassistant.settings.MailSettingsService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReminderScheduler {
    private final TaskRepository taskRepository;
    private final MailService mailService;
    private final AppLifecycle appLifecycle;
    private final MailSettingsService settingsService;

    public ReminderScheduler(TaskRepository taskRepository,
                             MailService mailService,
                             AppLifecycle appLifecycle,
                             MailSettingsService settingsService) {
        this.taskRepository = taskRepository;
        this.mailService = mailService;
        this.appLifecycle = appLifecycle;
        this.settingsService = settingsService;
    }

    @Scheduled(fixedDelay = 60_000)
    public void sendDueReminders() {
        if (!appLifecycle.ready()) {
            return;
        }
        for (TaskItem task : taskRepository.dueReminders(LocalDateTime.now())) {
            String content = """
                    事项提醒

                    标题：%s
                    分类：%s
                    优先级：%s
                    截止时间：%s

                    %s
                    """.formatted(
                    task.title(),
                    task.category() == null ? "-" : task.category(),
                    task.priority(),
                    task.dueAt() == null ? "-" : task.dueAt(),
                    task.description() == null ? "" : task.description());
            List<String> recipients = MailService.splitRecipients(settingsService.get().reminderRecipients());
            if (mailService.sendText(recipients, "备忘提醒：" + task.title(), content)) {
                taskRepository.markReminderSent(task.id());
            }
        }
    }
}
