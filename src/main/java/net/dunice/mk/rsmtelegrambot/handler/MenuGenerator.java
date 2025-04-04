package net.dunice.mk.rsmtelegrambot.handler;

import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.NEW_CHECK;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.PARTNERS_LIST;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.PERIOD_ANALYTICS;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.UPDATE_PROFILE;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.VERIFICATION_CODE;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.ADMIN_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.SUPERUSER_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.entity.Role.ADMIN;
import static net.dunice.mk.rsmtelegrambot.entity.Role.USER;

import lombok.RequiredArgsConstructor;
import net.dunice.mk.rsmtelegrambot.constant.Menu;
import net.dunice.mk.rsmtelegrambot.entity.Partner;
import net.dunice.mk.rsmtelegrambot.entity.Role;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MenuGenerator implements MessageGenerator {

    private final Map<Menu, ReplyKeyboard> menus;

    public SendMessage generateRoleSpecificMainMenu(Long telegramId, Role role) {
        Menu menu = role == USER ? MAIN_MENU :
            role == ADMIN ? ADMIN_MAIN_MENU :
                SUPERUSER_MAIN_MENU;
        return generateSendMessage(telegramId, "Выберите раздел:", menus.get(menu));
    }

    public ReplyKeyboardMarkup getPartnerMenu(Partner partner) {
        boolean isValid = partner.isValid();

        ReplyKeyboardMarkup replyMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(PARTNERS_LIST);
        row1.add(PERIOD_ANALYTICS);
        keyboard.add(row1);

        KeyboardRow row2 = new KeyboardRow();
        if (isValid) {
            row2.add(VERIFICATION_CODE);
        }
        row2.add(NEW_CHECK);
        keyboard.add(row2);

        KeyboardRow row3 = new KeyboardRow();
        row3.add(UPDATE_PROFILE);
        keyboard.add(row3);

        replyMarkup.setKeyboard(keyboard);
        replyMarkup.setResizeKeyboard(true);
        replyMarkup.setOneTimeKeyboard(false);

        return replyMarkup;
    }
}
