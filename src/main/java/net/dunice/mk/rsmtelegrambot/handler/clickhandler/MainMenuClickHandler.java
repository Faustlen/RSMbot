package net.dunice.mk.rsmtelegrambot.handler.clickhandler;

import net.dunice.mk.rsmtelegrambot.constants.InteractionState;
import org.jvnet.hk2.annotations.Service;

@Service
public class MainMenuClickHandler implements ClickHandler {
    @Override
    public String handleClick(String data, Long telegramId) {
        String response = switch (data) {
            case "Изменить профиль" -> "Вы выбрали: Изменить профиль";
            case "Партнеры" -> "Вы выбрали: Партнёры";
            case "Мероприятия" -> "Вы выбрали: Мероприятия";
            default -> "Неверная команда - " + data;
        };
        return response;
    }

    @Override
    public InteractionState getState() {
        return InteractionState.USER_MAIN_MENU;
    }
}
