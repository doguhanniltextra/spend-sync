package com.enterprise.spendsync.notification.internal.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
@ConditionalOnBean(JavaMailSender.class)
public class SmtpEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailService.class);

    private final JavaMailSender mailSender;
    private final EmailTemplateRenderer templateRenderer;

    @Value("${spring.mail.from:no-reply@spendsync.enterprise.com}")
    private String defaultFrom;

    public SmtpEmailService(JavaMailSender mailSender, EmailTemplateRenderer templateRenderer) {
        this.mailSender = mailSender;
        this.templateRenderer = templateRenderer;
    }

    @Override
    public void sendSimpleEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(defaultFrom);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Sent simple email to: {}, Subject: {}", to, subject);
        } catch (Exception e) {
            log.error("Failed to send simple email to {}: {}", to, e.getMessage(), e);
        }
    }

    @Override
    public void sendTemplatedEmail(String to, String subject, String templateName, Map<String, Object> templateModel) {
        try {
            String htmlContent = templateRenderer.render(templateName, templateModel);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());

            helper.setFrom(defaultFrom);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            log.info("Sent HTML templated email to: {}, Template: {}, Subject: {}", to, templateName, subject);
        } catch (MessagingException e) {
            log.error("Messaging exception while preparing email for {}: {}", to, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error dispatching email to {}: {}", to, e.getMessage(), e);
        }
    }
}
