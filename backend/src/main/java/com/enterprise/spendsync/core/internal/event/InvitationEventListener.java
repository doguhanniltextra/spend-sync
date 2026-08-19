package com.enterprise.spendsync.core.internal.event;

import com.enterprise.spendsync.shared.notification.EmailService;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class InvitationEventListener {

    private final EmailService emailService;

    public InvitationEventListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @Async
    @EventListener
    public void handleSubAccountInvited(SubAccountInvitedEvent event) {
        String subject = event.companyName() + " - You are invited to SpendSync Platform";

        Map<String, Object> model = Map.of(
                "Company", event.companyName(),
                "Legal Entity", event.legalEntityName(),
                "Assigned Roles", event.targetRoles().toString(),
                "Invitation Link", event.inviteUrl(),
                "Expires At", event.expiresAt().toString(),
                "Security Note", "This link is single-use only. Please do not share it with unauthorized parties."
        );

        emailService.sendTemplatedEmail(
                event.recipientEmail(),
                subject,
                "subaccount-invite",
                model
        );
    }
}
