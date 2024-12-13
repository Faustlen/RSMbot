package net.dunice.mk.rsmtelegrambot.handler;

import static net.dunice.mk.rsmtelegrambot.constant.Menu.ADMIN_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.SUPERUSER_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.entity.Role.ADMIN;
import static net.dunice.mk.rsmtelegrambot.entity.Role.USER;

import lombok.RequiredArgsConstructor;
import net.dunice.mk.rsmtelegrambot.handler.state.BasicState;
import net.dunice.mk.rsmtelegrambot.constant.Menu;
import net.dunice.mk.rsmtelegrambot.entity.Role;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class MenuGenerator implements MessageSender {

    private final Map<Long, BasicState> states;
    private final Map<Menu, ReplyKeyboardMarkup> menus;

    public SendMessage generateRoleSpecificMainMenu(Long telegramId, Role role) {
        states.put(telegramId, BasicState.IN_MAIN_MENU);
        Menu menu = role == USER ? MAIN_MENU :
            role == ADMIN ? ADMIN_MAIN_MENU :
                SUPERUSER_MAIN_MENU;
        return generateSendMessage(telegramId, "Выберите действие:", menus.get(menu));
    }
}
