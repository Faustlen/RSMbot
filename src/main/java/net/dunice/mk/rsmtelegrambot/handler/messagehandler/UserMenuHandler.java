package net.dunice.mk.rsmtelegrambot.handler.messagehandler;

import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.ADD_EVENT;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.ADD_PARTNER;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.ADMINS_LIST;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.EVENTS_LIST;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.PARTNERS_LIST;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.SET_ADMIN;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.UPDATE_PROFILE;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.USERS_LIST;
import static net.dunice.mk.rsmtelegrambot.entity.Role.SUPER_USER;
import static net.dunice.mk.rsmtelegrambot.entity.Role.USER;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.CHANGE_PROFILE;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.CREATE_EVENT;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.GRANT_ADMIN;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.IN_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.SHOW_ADMINS;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.SHOW_EVENTS;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.SHOW_PARTNERS;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.SHOW_USERS;

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
    private final ShowEventsHandler showEventsHandler;
    private final ShowPartnersHandler showPartnersHandler;
    private final ShowAdminsHandler showAdminsHandler;
    private final CreateEventHandler createEventHandler;
    private final ShowUsersHandler showUsersHandler;
    private final Map<Long, BasicState> basicStates;

    @Override
    public PartialBotApiMethod<Message> handle(MessageDto messageDto, Long telegramId) {
        Role role = userRepository.findByTelegramId(telegramId).get().getUserRole();
        String text = messageDto.getText();
        Optional<PartialBotApiMethod<Message>> sendMessage = Optional.ofNullable(
            switch (text) {
                case UPDATE_PROFILE -> {
                    basicStates.put(telegramId, CHANGE_PROFILE);
                    yield updateProfileHandler.handle(messageDto, telegramId);
                }
                case PARTNERS_LIST -> {
                    basicStates.put(telegramId, SHOW_PARTNERS);
                    yield showPartnersHandler.handle(messageDto, telegramId);
                }
                case EVENTS_LIST -> {
                    basicStates.put(telegramId, SHOW_EVENTS);
                    yield showEventsHandler.handle(messageDto, telegramId);
                }
                default -> null;
            }
        );
        if (sendMessage.isEmpty() && role != USER) {
            sendMessage = Optional.ofNullable(
                switch (text) {
                    case ADD_EVENT -> {
                        basicStates.put(telegramId, CREATE_EVENT);
                        yield createEventHandler.handle(messageDto, telegramId);
                    }
                    case USERS_LIST -> {
                        basicStates.put(telegramId, SHOW_USERS);
                        yield showUsersHandler.handle(messageDto, telegramId);
                    }
                    default -> null;
                });
            if (sendMessage.isEmpty() && role == SUPER_USER) {
                sendMessage = Optional.ofNullable(
                    switch (text) {
                        case SET_ADMIN -> {
                            basicStates.put(telegramId, GRANT_ADMIN);
                            yield grantAdminHandler.handle(messageDto, telegramId);
                        }
                        case ADMINS_LIST -> {
                            basicStates.put(telegramId, SHOW_ADMINS);
                            yield showAdminsHandler.handle(messageDto, telegramId);
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