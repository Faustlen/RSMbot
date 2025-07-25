package net.dunice.mk.rsmtelegrambot.handler.messagehandler;

import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.*;
import static net.dunice.mk.rsmtelegrambot.entity.Role.SUPER_USER;
import static net.dunice.mk.rsmtelegrambot.entity.Role.USER;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.BasicStep;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.BasicStep.*;

import lombok.RequiredArgsConstructor;
import net.dunice.mk.rsmtelegrambot.dto.MessageDto;
import net.dunice.mk.rsmtelegrambot.handler.state.BasicState;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserMenuHandler implements MessageHandler {

    private final UpdateProfileHandler updateProfileHandler;
    private final ShowEventsHandler showEventsHandler;
    private final ShowPartnersHandler showPartnersHandler;
    private final ShowAdminsHandler showAdminsHandler;
    private final ShowAnalyticsHandler showAnalyticsHandler;
    private final CreateEventHandler createEventHandler;
    private final ShowUsersHandler showUsersHandler;
    private final MessageBroadcastHandler messageBroadcastHandler;
    private final ShowStocksHandler showStocksHandler;
    private final Map<Long, BasicState> basicStates;

    @Override
    public PartialBotApiMethod<Message> handle(MessageDto messageDto, Long telegramId) {
        String text = messageDto.getText();
        BasicState state = basicStates.get(telegramId);
        if (text == null) {
            return generateSendMessage(telegramId, "Неверная команда");
        }
        Optional<PartialBotApiMethod<Message>> sendMessage = Optional.ofNullable(
            switch (text) {
                case UPDATE_PROFILE -> {
                    state.setStep(CHANGE_PROFILE);
                    yield updateProfileHandler.handle(messageDto, telegramId);
                }
                case PARTNERS_LIST -> {
                    state.setStep(SHOW_PARTNERS);
                    yield showPartnersHandler.handle(messageDto, telegramId);
                }
                case EVENTS_LIST -> {
                    state.setStep(SHOW_EVENTS);
                    yield showEventsHandler.handle(messageDto, telegramId);
                }
                case STOCKS_LIST -> {
                    state.setStep(SHOW_STOCKS);
                    yield showStocksHandler.handle(messageDto, telegramId);
                }
                default -> null;
            }
        );
        if (sendMessage.isEmpty() && state.getUser().getUserRole() != USER) {
            sendMessage = Optional.ofNullable(
                switch (text) {
                    case ADD_EVENT -> {
                        state.setStep(CREATE_EVENT);
                        yield createEventHandler.handle(messageDto, telegramId);
                    }
                    case USERS_LIST -> {
                        state.setStep(SHOW_USERS);
                        yield showUsersHandler.handle(messageDto, telegramId);
                    }
                    case SEND_MESSAGE_TO_EVERYONE -> {
                        state.setStep(SEND_MESSAGE_TO_EVERYBODY);
                        yield messageBroadcastHandler.handle(messageDto, telegramId);
                    }
                    default -> null;
                });
            if (sendMessage.isEmpty() && state.getUser().getUserRole() == SUPER_USER) {
                sendMessage = Optional.ofNullable(
                    switch (text) {
                        case ADMINS_LIST -> {
                            state.setStep(SHOW_ADMINS);
                            yield showAdminsHandler.handle(messageDto, telegramId);
                        }
                        case PERIOD_ANALYTICS -> {
                            state.setStep(SHOW_ANALYTICS);
                            yield showAnalyticsHandler.handle(messageDto, telegramId);
                        }
                        default -> null;
                    });
            }
        }
        return sendMessage.orElseGet(() ->
            generateSendMessage(telegramId, "Неверная команда - " + text));
    }

    @Override
    public BasicStep getStep() {
        return IN_MAIN_MENU;
    }
}