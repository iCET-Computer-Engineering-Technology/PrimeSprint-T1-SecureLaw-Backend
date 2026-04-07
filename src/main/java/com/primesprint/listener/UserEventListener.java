package com.primesprint.listener;

import com.primesprint.event.UserCreatedEvent;
import com.primesprint.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventListener {
    private final EmailService emailService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserCreatedEvent(UserCreatedEvent event) {
        log.info("Transaction committed. Triggering invitation email for: {}", event.email());
        emailService.sendInvitationEmail(
                event.email(),
                event.accessLink(),
                event.username(),
                event.temporaryPassword(),
                event.createdAt()
        );
    }
}
