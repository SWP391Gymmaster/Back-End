package com.swp391_be.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class MailSettingsValidator implements ApplicationRunner {
    private final Environment environment;

    public MailSettingsValidator(Environment environment) { this.environment = environment; }

    @Override
    public void run(ApplicationArguments args) {
        for (String profile : environment.getActiveProfiles()) {
            if ("production".equals(profile)) validateProductionSettings();
        }
    }

    private void validateProductionSettings() {
        String host = environment.getProperty("spring.mail.host", "");
        String from = environment.getProperty("MAIL_FROM", "");
        String baseUrl = environment.getProperty("app.frontend.base-url", "");
        if (host.isBlank() || from.isBlank() || !baseUrl.startsWith("https://")) {
            throw new IllegalStateException("Production requires SMTP_HOST, MAIL_FROM, and an HTTPS FRONTEND_BASE_URL");
        }
    }
}
