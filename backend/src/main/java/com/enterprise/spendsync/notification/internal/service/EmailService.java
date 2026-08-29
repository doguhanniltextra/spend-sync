package com.enterprise.spendsync.notification.internal.service;

import java.util.Map;

/**
 * Enterprise Email Dispatcher Service Contract.
 */
public interface EmailService {

    void sendSimpleEmail(String to, String subject, String body);

    void sendTemplatedEmail(String to, String subject, String templateName, Map<String, Object> templateModel);
}
