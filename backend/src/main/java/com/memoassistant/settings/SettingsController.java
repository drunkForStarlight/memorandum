package com.memoassistant.settings;

import java.util.List;

import com.memoassistant.common.MailService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings/mail")
public class SettingsController {
    private final MailSettingsService settingsService;
    private final MailService mailService;

    public SettingsController(MailSettingsService settingsService, MailService mailService) {
        this.settingsService = settingsService;
        this.mailService = mailService;
    }

    @GetMapping
    public SettingsDtos.MailSettingsResponse get() {
        return settingsService.publicSettings();
    }

    @PutMapping
    public SettingsDtos.MailSettingsResponse update(@RequestBody SettingsDtos.MailSettingsRequest request) {
        return settingsService.update(request);
    }

    @PostMapping("/test")
    public SettingsDtos.TestMailResponse test(@RequestBody SettingsDtos.TestMailRequest request) {
        boolean ok = mailService.sendText(
                List.of(request.recipient()),
                "备忘助手测试邮件",
                "如果你收到这封邮件，说明 SMTP 配置已经可用。");
        return new SettingsDtos.TestMailResponse(ok, ok ? "测试邮件已发送" : "测试邮件发送失败，请检查 SMTP 配置和后端日志");
    }
}

