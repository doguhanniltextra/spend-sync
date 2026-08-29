package com.enterprise.spendsync.notification.internal.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Local & Development Console Email Dispatcher.
 * Formats email notifications in logs with actionable invite links for pair-programming and local testing.
 */
@Service
@ConditionalOnMissingBean(JavaMailSender.class)
public class ConsoleEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(ConsoleEmailService.class);

    @Override
    public void sendSimpleEmail(String to, String subject, String body) {
        log.info("""
                
                ========================= [OUTGOING EMAIL NOTIFICATION] =========================
                TO: {}
                SUBJECT: {}
                BODY:
                {}
                =================================================================================
                """, to, subject, body);
    }

    @Override
    public void sendTemplatedEmail(String to, String subject, String templateName, Map<String, Object> templateModel) {
        StringBuilder sb = new StringBuilder();
        if (templateModel != null) {
            templateModel.forEach((k, v) -> sb.append(String.format("  • %s: %s\n", k, v)));
        }

        log.info("""
                
                ========================= [OUTGOING TEMPLATED EMAIL] =========================
                TO: {}
                SUBJECT: {}
                TEMPLATE: {}
                DATA:
                {}
                ==============================================================================
                """, to, subject, templateName, sb);
    }
}
