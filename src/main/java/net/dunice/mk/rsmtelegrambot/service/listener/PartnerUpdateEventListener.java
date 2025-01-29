package net.dunice.mk.rsmtelegrambot.service.listener;

import lombok.RequiredArgsConstructor;
import net.dunice.mk.rsmtelegrambot.event.PartnerUpdateEvent;
import net.dunice.mk.rsmtelegrambot.repository.UserRepository;
import net.dunice.mk.rsmtelegrambot.service.TelegramBot;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class PartnerUpdateEventListener extends AbstractPartnerEventListener {

    public PartnerUpdateEventListener(TelegramBot telegramBot, UserRepository userRepository) {
        super(telegramBot, userRepository);
    }

    @EventListener
    public void handlePartnerUpdateEvent(PartnerUpdateEvent event) {
        processPartnerEvent(event.getPartner(), "Новая заявка на изменение данных партнёра:");
    }
}
