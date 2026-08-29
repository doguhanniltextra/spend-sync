package com.enterprise.spendsync.notification.internal.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

@Component
public class EmailTemplateRenderer {

    private static final Logger log = LoggerFactory.getLogger(EmailTemplateRenderer.class);
    private final TemplateEngine templateEngine;

    public EmailTemplateRenderer(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public String render(String templateName, Map<String, Object> templateModel) {
        try {
            Context context = new Context();
            if (templateModel != null) {
                templateModel.forEach(context::setVariable);
            }
            String templatePath = templateName.startsWith("mail/") ? templateName : "mail/" + templateName;
            return templateEngine.process(templatePath, context);
        } catch (Exception ex) {
            log.warn("Failed to render Thymeleaf template '{}'. Falling back to plain text format. Error: {}", templateName, ex.getMessage());
            StringBuilder sb = new StringBuilder();
            sb.append("<html><body><h2>SpendSync Notification</h2><ul>");
            if (templateModel != null) {
                templateModel.forEach((k, v) -> sb.append("<li><strong>").append(k).append(":</strong> ").append(v).append("</li>"));
            }
            sb.append("</ul></body></html>");
            return sb.toString();
        }
    }
}
