package net.dunice.mk.rsmtelegrambot.service.listener;

import lombok.RequiredArgsConstructor;
import net.dunice.mk.rsmtelegrambot.event.PartnerRegisteredEvent;
import net.dunice.mk.rsmtelegrambot.repository.UserRepository;
import net.dunice.mk.rsmtelegrambot.service.TelegramBot;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class PartnerRegistrationEventListener extends AbstractPartnerEventListener {

    public PartnerRegistrationEventListener(TelegramBot telegramBot, UserRepository userRepository) {
        super(telegramBot, userRepository);
    }

    @EventListener
    public void handlePartnerRegistrationEvent(PartnerRegisteredEvent event) {
        processPartnerEvent(event.getPartner(), "Новая заявка на регистрацию партнёра:");
    }
}
