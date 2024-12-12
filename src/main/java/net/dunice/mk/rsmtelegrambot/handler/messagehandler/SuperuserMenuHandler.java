package net.dunice.mk.rsmtelegrambot.handler.messagehandler;

import lombok.RequiredArgsConstructor;
import net.dunice.mk.rsmtelegrambot.constant.InteractionState;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.Map;

import static net.dunice.mk.rsmtelegrambot.constant.InteractionState.*;


@Service
@RequiredArgsConstructor
public class SuperuserMenuHandler implements MessageHandler {

    private final Map<Long, InteractionState> interactionStates;
    private final ProfileUpdateHandler profileUpdateHandler;

    @Override
    public SendMessage handleMessage(String message, Long telegramId) {
        return  switch (message) {
            case "Изменить профиль" -> {
                interactionStates.put(telegramId, CHANGE_PROFILE);
                yield profileUpdateHandler.handleMessage(message, telegramId);
            }
            case "Партнеры" -> generateSendMessage(telegramId,"Вы выбрали: Партнеры");
            case "Мероприятия" -> generateSendMessage(telegramId, "Вы выбрали: Мероприятия");
            case "Назначить админа" -> {
                interactionStates.put(telegramId, GRANT_ADMIN);
                yield generateSendMessage(telegramId, "Введите ID пользователя, которому хотите дать права администратора");
            }
            case "Добавить партнера" -> generateSendMessage(telegramId, "Вы выбрали: Добавить партнера");
            case "Добавить мероприятие" -> generateSendMessage(telegramId, "Вы выбрали: Добавить мероприятие");
            default -> generateSendMessage(telegramId, "Неверная команда - " + message);
        };
    }

    @Override
    public InteractionState getState() {
        return IN_SUPERUSER_MAIN_MENU;
    }
}
