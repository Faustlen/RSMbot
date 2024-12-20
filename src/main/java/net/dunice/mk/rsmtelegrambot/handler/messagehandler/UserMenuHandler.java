package net.dunice.mk.rsmtelegrambot.handler.messagehandler;

import static net.dunice.mk.rsmtelegrambot.entity.Role.SUPER_USER;
import static net.dunice.mk.rsmtelegrambot.entity.Role.USER;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.BAN_USER;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.CHANGE_PROFILE;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.CREATE_EVENT;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.GRANT_ADMIN;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.IN_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.SHOW_EVENTS;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.SHOW_PARTNERS;

import lombok.RequiredArgsConstructor;
import net.dunice.mk.rsmtelegrambot.dto.MessageDto;
import net.dunice.mk.rsmtelegrambot.entity.Role;
import net.dunice.mk.rsmtelegrambot.handler.state.BasicState;
import net.dunice.mk.rsmtelegrambot.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserMenuHandler implements MessageHandler {

    private final UserRepository userRepository;
    private final UpdateProfileHandler updateProfileHandler;
    private final GrantAdminHandler grantAdminHandler;
    private final BanUserHandler banUserHandler;
    private final ShowEventsHandler showEventsHandler;
    private final ShowPartnersHandler showPartnersHandler;
    private final CreateEventHandler createEventHandler;
    private final Map<Long, BasicState> basicStates;

    @Override
    public PartialBotApiMethod<Message> handle(MessageDto messageDto, Long telegramId) {
        Role role = userRepository.findByTelegramId(telegramId).get().getUserRole();
        String text = messageDto.getText();
        Optional<PartialBotApiMethod<Message>> sendMessage = Optional.ofNullable(
            switch (text) {
                case "Изменить профиль" -> {
                    basicStates.put(telegramId, CHANGE_PROFILE);
                    yield updateProfileHandler.handle(messageDto, telegramId);
                }
                case "Партнеры" -> {
                    basicStates.put(telegramId, SHOW_PARTNERS);
                    yield showPartnersHandler.handle(messageDto, telegramId);
                }
                case "Мероприятия" -> {
                    basicStates.put(telegramId, SHOW_EVENTS);
                    yield showEventsHandler.handle(messageDto, telegramId);
                }
                default -> null;
            }
        );
        if (sendMessage.isEmpty() && role != USER) {
            sendMessage = Optional.ofNullable(
                switch (text) {
                    case "Добавить партнера" -> generateSendMessage(telegramId, "Вы выбрали: Добавить партнера");
                    case "Добавить мероприятие" -> {
                        basicStates.put(telegramId, CREATE_EVENT);
                        yield createEventHandler.handle(messageDto, telegramId);
                    }
                    case "Забанить пользователя" -> {
                        basicStates.put(telegramId, BAN_USER);
                        yield banUserHandler.handle(messageDto, telegramId);
                    }
                    default -> null;
                });
            if (sendMessage.isEmpty() && role == SUPER_USER) {
                sendMessage = Optional.ofNullable(
                    switch (text) {
                        case "Назначить админа" -> {
                            basicStates.put(telegramId, GRANT_ADMIN);
                            yield grantAdminHandler.handle(messageDto, telegramId);
                        }
                        default -> null;
                    });
            }
        }
        return sendMessage.orElseGet(() ->
            generateSendMessage(telegramId, "Неверная команда - " + text));
    }

    @Override
    public BasicState getState() {
        return IN_MAIN_MENU;
    }
}