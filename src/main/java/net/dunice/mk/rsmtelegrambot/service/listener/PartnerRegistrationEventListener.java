package net.dunice.mk.rsmtelegrambot.service.listener;

import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.ACCEPT_PARTNER_REGISTRATION;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.DECLINE_PARTNER_REGISTRATION;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.TO_MAIN_MENU;
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
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
        ReplyKeyboard replyKeyboard = getApprovalMenu();
        List<User> admins = userRepository.findAllByUserRole(ADMIN);
        admins.forEach(admin -> telegramBot.sendMessage(
            generateSendMessage(admin.getTelegramId(), notification, replyKeyboard)));

    }

    private ReplyKeyboard getApprovalMenu() {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        InlineKeyboardButton toMainMenuButton = new InlineKeyboardButton(TO_MAIN_MENU);
        toMainMenuButton.setCallbackData(toMainMenuButton.getText());
        InlineKeyboardButton acceptPartnerRegistrationButton = new InlineKeyboardButton(ACCEPT_PARTNER_REGISTRATION);
        acceptPartnerRegistrationButton.setCallbackData(acceptPartnerRegistrationButton.getText());
        InlineKeyboardButton declinePartnerRegistrationButton = new InlineKeyboardButton(DECLINE_PARTNER_REGISTRATION);
        declinePartnerRegistrationButton.setCallbackData(declinePartnerRegistrationButton.getText());
        keyboard.add(List.of(toMainMenuButton, acceptPartnerRegistrationButton, declinePartnerRegistrationButton));
        inlineKeyboardMarkup.setKeyboard(keyboard);
        return inlineKeyboardMarkup;
    }
}
