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
        String subject = event.companyName() + " - SpendSync Platformuna Davet Edildiniz";

        Map<String, Object> model = Map.of(
                "Şirket", event.companyName(),
                "Tüzel Kişilik", event.legalEntityName(),
                "Atanan Roller", event.targetRoles().toString(),
                "Davet Bağlantısı", event.inviteUrl(),
                "Geçerlilik Bitiş", event.expiresAt().toString(),
                "Güvenlik Notu", "Bu bağlantı tek kullanımlıktır. Lütfen başkalarıyla paylaşmayınız."
        );

        emailService.sendTemplatedEmail(
                event.recipientEmail(),
                subject,
                "subaccount-invite",
                model
        );
    }
}
