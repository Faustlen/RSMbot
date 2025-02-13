package net.dunice.mk.rsmtelegrambot.service.listener;

import lombok.RequiredArgsConstructor;
import net.dunice.mk.rsmtelegrambot.entity.Partner;
import net.dunice.mk.rsmtelegrambot.entity.User;
import net.dunice.mk.rsmtelegrambot.handler.MessageGenerator;
import net.dunice.mk.rsmtelegrambot.repository.UserRepository;
import net.dunice.mk.rsmtelegrambot.service.TelegramBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.CONFIRM;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.REJECT;
import static net.dunice.mk.rsmtelegrambot.entity.Role.ADMIN;

@RequiredArgsConstructor
public abstract class AbstractPartnerEventListener implements MessageGenerator {
    private static final String PARTNER_INFO_TEMPLATE = """
        %s
        ID партнера: %s
        Название: %s
        Телефон: %s
        Адресс: %s
        Скидка: %s%%
        Категория: %s
        Дата окончания скидки: %s
        Информация о партнере: %s
        """;

    protected final TelegramBot telegramBot;
    protected final UserRepository userRepository;

    protected void processPartnerEvent(Partner partner, String eventDescription) {
        String notification = PARTNER_INFO_TEMPLATE.formatted(
            eventDescription,
            partner.getPartnerTelegramId(),
            partner.getName(),
            partner.getPhoneNumber(),
            getHyperlinkFromAddress(partner.getAddress()),
            partner.getDiscountPercent(),
            partner.getCategory().getCategoryName(),
            partner.getDiscountDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            partner.getPartnersInfo()
        );

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<InlineKeyboardButton> buttons = List.of(
            InlineKeyboardButton.builder().text(CONFIRM)
                .callbackData("broadcast_partnerAccept_confirm_" + partner.getPartnerTelegramId()).build(),
            InlineKeyboardButton.builder().text(REJECT)
                .callbackData("broadcast_partnerAccept_reject_" + partner.getPartnerTelegramId()).build()
        );
        markup.setKeyboard(List.of(buttons));

        List<User> admins = userRepository.findAllByUserRole(ADMIN);
        admins.forEach(admin -> {
            SendMessage sendMessage = generateSendMessage(admin.getTelegramId(), notification, markup);
            sendMessage.setParseMode("HTML");
            telegramBot.sendNoDeleteMessage(sendMessage);
        });
    }

    private String getHyperlinkFromAddress(String address) {
        String encodedAddress = URLEncoder.encode(address, StandardCharsets.UTF_8);
        String url = "https://yandex.ru/maps/?text=" + encodedAddress;
        return String.format("<a href=\"%s\">%s</a>", url, address);
    }
}
