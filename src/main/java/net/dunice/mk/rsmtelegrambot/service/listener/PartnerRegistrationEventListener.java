package net.dunice.mk.rsmtelegrambot.service.listener;

import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.CONFIRM;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.REJECT;
import static net.dunice.mk.rsmtelegrambot.entity.Role.ADMIN;

import lombok.RequiredArgsConstructor;
import net.dunice.mk.rsmtelegrambot.entity.Partner;
import net.dunice.mk.rsmtelegrambot.entity.User;
import net.dunice.mk.rsmtelegrambot.event.PartnerRegisteredEvent;
import net.dunice.mk.rsmtelegrambot.handler.MessageGenerator;
import net.dunice.mk.rsmtelegrambot.repository.UserRepository;
import net.dunice.mk.rsmtelegrambot.service.TelegramBot;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PartnerRegistrationEventListener implements MessageGenerator {

    private static final String PARTNER_INFO_TEMPLATE = """
        Новая заявка на регистрацию партнёра:
        ID партнера: %s
        Название: %s
        Телефон: %s
        Скидка: %s%%
        Категория: %s
        Дата окончания скидки: %s
        Информация о партнере: %s
        """;
    private final TelegramBot telegramBot;
    private final UserRepository userRepository;

    @EventListener
    public void handlePartnerRegistrationEvent(PartnerRegisteredEvent event) {
        Partner partner = event.getPartner();
        String notification = PARTNER_INFO_TEMPLATE.formatted(
            partner.getPartnerTelegramId(),
            partner.getName(),
            partner.getPhoneNumber(),
            partner.getDiscountPercent(),
            partner.getCategory().getCategoryName(),
            partner.getDiscountDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            partner.getPartnersInfo());

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<InlineKeyboardButton> buttons = List.of(
            InlineKeyboardButton.builder().text(CONFIRM)
                .callbackData("broadcast_partnerAccept_confirm_" + partner.getPartnerTelegramId()).build(),
            InlineKeyboardButton.builder().text(REJECT)
                .callbackData("broadcast_partnerAccept_reject_" + partner.getPartnerTelegramId()).build()
        );
        markup.setKeyboard(List.of(buttons));

        List<User> admins = userRepository.findAllByUserRole(ADMIN);
        admins.forEach(admin ->
            telegramBot.sendNoDeleteMessage(generateSendMessage(admin.getTelegramId(), notification, markup)));
    }
}
