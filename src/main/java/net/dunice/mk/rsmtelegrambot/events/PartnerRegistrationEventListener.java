package net.dunice.mk.rsmtelegrambot.events;

import lombok.RequiredArgsConstructor;
import net.dunice.mk.rsmtelegrambot.entity.Partner;
import net.dunice.mk.rsmtelegrambot.entity.Role;
import net.dunice.mk.rsmtelegrambot.entity.User;
import net.dunice.mk.rsmtelegrambot.handler.MessageGenerator;
import net.dunice.mk.rsmtelegrambot.repository.UserRepository;
import net.dunice.mk.rsmtelegrambot.service.TelegramBot;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PartnerRegistrationEventListener implements MessageGenerator {

    private final TelegramBot telegramBot;

    private final UserRepository userRepository;

    private static final String PARTNER_INFO_TEMPLATE = """
        Новая заявка на регистрацию партнёра:
        Название: %s
        Телефон: %s
        Скидка: %s%%
        Категория: %s
        """;

    @EventListener
    public void handlePartnerRegistrationEvent(PartnerRegisteredEvent event) {
        Partner partner = event.getPartner();
        String notification = String.format(PARTNER_INFO_TEMPLATE,
            partner.getName(),
            partner.getPhoneNumber(),
            partner.getDiscountPercent(),
            partner.getCategory().getCategoryName());
        List<User> admins = userRepository.findAllByUserRole(Role.ADMIN);
        admins.forEach(admin -> telegramBot.sendMessage(generateSendMessage(admin.getTelegramId(), notification)));
    }
}
