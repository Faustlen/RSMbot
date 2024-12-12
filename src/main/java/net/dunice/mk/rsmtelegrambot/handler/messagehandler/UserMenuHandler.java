package net.dunice.mk.rsmtelegrambot.handler.messagehandler;

import static net.dunice.mk.rsmtelegrambot.constant.InteractionState.CHANGE_PROFILE;
import static net.dunice.mk.rsmtelegrambot.constant.InteractionState.IN_USER_MAIN_MENU;

import lombok.RequiredArgsConstructor;
import net.dunice.mk.rsmtelegrambot.constant.InteractionState;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.Map;


@Service
@RequiredArgsConstructor
public class UserMenuHandler implements MessageHandler {

    private final Map<Long, InteractionState> interactionStates;
    private final ProfileUpdateHandler profileUpdateHandler;

    @Override
    public SendMessage handleMessage(String message, Long telegramId) {
        return switch (message) {
            case "Изменить профиль" -> {
                interactionStates.put(telegramId, CHANGE_PROFILE);
                yield profileUpdateHandler.handleMessage(message, telegramId);
            }
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