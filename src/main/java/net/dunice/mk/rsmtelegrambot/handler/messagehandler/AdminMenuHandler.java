package net.dunice.mk.rsmtelegrambot.handler.messagehandler;

import lombok.RequiredArgsConstructor;
import net.dunice.mk.rsmtelegrambot.constant.InteractionState;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.Map;

import static net.dunice.mk.rsmtelegrambot.constant.InteractionState.CHANGE_PROFILE;
import static net.dunice.mk.rsmtelegrambot.constant.InteractionState.IN_ADMIN_MAIN_MENU;

@Service
@RequiredArgsConstructor
public class AdminMenuHandler implements MessageHandler {

    private final ProfileUpdateHandler profileUpdateHandler;
    private final Map<Long, InteractionState> interactionStates;

    @Override
    public SendMessage handleMessage(String message, Long telegramId) {
        return  switch (message) {
            case "Изменить профиль" -> {
                interactionStates.put(telegramId, CHANGE_PROFILE);
                yield profileUpdateHandler.handleMessage(message, telegramId);
            }
            case "Партнеры" -> generateSendMessage(telegramId,"Вы выбрали: Партнеры");
            case "Мероприятия" -> generateSendMessage(telegramId, "Вы выбрали: Мероприятия");
            case "Добавить партнера" -> generateSendMessage(telegramId, "Вы выбрали: Добавить партнера");
            case "Добавить мероприятие" -> generateSendMessage(telegramId, "Вы выбрали: Добавить мероприятие");
            default -> generateSendMessage(telegramId, "Неверная команда - " + message);
        };
    }

    @Override
    public InteractionState getState() {
        return IN_ADMIN_MAIN_MENU;
    }
}
