package net.dunice.mk.rsmtelegrambot.handler;

import lombok.RequiredArgsConstructor;
import net.dunice.mk.rsmtelegrambot.constant.InteractionState;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.Map;

import static net.dunice.mk.rsmtelegrambot.constant.InteractionState.IN_USER_MAIN_MENU;


@Service
@RequiredArgsConstructor
public class UserMenuHandler implements MessageHandler {

    private final GrantAdminHandler grantAdminHandler;
    private final Map<Long, InteractionState> interactionStates;

    @Override
    public SendMessage handleMessage(String message, Long telegramId) {
        return switch (message) {
            case "Изменить профиль" -> generateSendMessage(telegramId, "Вы выбрали: Изменить профиль");
            case "Партнеры" -> generateSendMessage(telegramId, "Вы выбрали: Партнеры");
            case "Мероприятия" -> generateSendMessage(telegramId, "Вы выбрали: Мероприятия");
            default -> generateSendMessage(telegramId, "Неверная команда - " + message);
        };
    }

    @Override
    public InteractionState getState() {
        return IN_USER_MAIN_MENU;
    }
}