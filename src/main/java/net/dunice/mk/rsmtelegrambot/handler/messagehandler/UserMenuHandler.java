package net.dunice.mk.rsmtelegrambot.handler.messagehandler;

import static net.dunice.mk.rsmtelegrambot.entity.Role.SUPER_USER;
import static net.dunice.mk.rsmtelegrambot.entity.Role.USER;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.CHANGE_PROFILE;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.GRANT_ADMIN;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.IN_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.SHOW_EVENTS;

import lombok.RequiredArgsConstructor;
import net.dunice.mk.rsmtelegrambot.entity.Role;
import net.dunice.mk.rsmtelegrambot.handler.state.BasicState;
import net.dunice.mk.rsmtelegrambot.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserMenuHandler implements MessageHandler {

    private final UserRepository userRepository;
    private final UpdateProfileHandler updateProfileHandler;
    private final GrantAdminHandler grantAdminHandler;
    private final ShowEventsHandler showEventsHandler;
    private final Map<Long, BasicState> basicStates;

    @Override
    public SendMessage handle(String message, Long telegramId) {
        Role role = userRepository.findByTelegramId(telegramId).get().getUserRole();
        Optional<SendMessage> sendMessage = Optional.ofNullable(
            switch (message) {
                case "Изменить профиль" -> {
                    basicStates.put(telegramId, CHANGE_PROFILE);
                    yield updateProfileHandler.handle(message, telegramId);
                }
                case "Партнеры" -> generateSendMessage(telegramId, "Вы выбрали: Партнеры");
                case "Мероприятия" -> {
                    basicStates.put(telegramId, SHOW_EVENTS);
                    yield showEventsHandler.handle(message, telegramId);
                }
                default -> null;
            }
        );
        if (sendMessage.isEmpty() && role != USER) {
            sendMessage = Optional.ofNullable(
                switch (message) {
                    case "Добавить партнера" -> generateSendMessage(telegramId, "Вы выбрали: Добавить партнера");
                    case "Добавить мероприятие" -> generateSendMessage(telegramId, "Вы выбрали: Добавить мероприятие");
                    default -> null;
                });
            if (sendMessage.isEmpty() && role == SUPER_USER) {
                sendMessage = Optional.ofNullable(
                    switch (message) {
                        case "Назначить админа" -> {
                            basicStates.put(telegramId, GRANT_ADMIN);
                            yield grantAdminHandler.handle(message, telegramId);
                        }
                        default -> null;
                    });
            }
        }
        return sendMessage.orElseGet(() ->
            generateSendMessage(telegramId, "Неверная команда - " + message));
    }

    @Override
    public BasicState getState() {
        return IN_MAIN_MENU;
    }
}