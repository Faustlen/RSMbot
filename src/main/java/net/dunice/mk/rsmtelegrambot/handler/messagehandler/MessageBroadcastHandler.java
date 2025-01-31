package net.dunice.mk.rsmtelegrambot.handler.messagehandler;

import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.TO_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.GO_TO_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.SELECTION_MENU;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.BasicStep.IN_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.BasicStep.SEND_MESSAGE_TO_EVERYBODY;
import static net.dunice.mk.rsmtelegrambot.handler.state.MessageBroadcastState.MessageBroadcastStep.CONFIRM_MESSAGE_TEXT;
import static net.dunice.mk.rsmtelegrambot.handler.state.MessageBroadcastState.MessageBroadcastStep.REQUEST_MESSAGE_TEXT;
import static net.dunice.mk.rsmtelegrambot.handler.state.MessageBroadcastState.MessageBroadcastStep.VERIFY_MESSAGE_TEXT;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dunice.mk.rsmtelegrambot.constant.Menu;
import net.dunice.mk.rsmtelegrambot.dto.MessageDto;
import net.dunice.mk.rsmtelegrambot.entity.User;
import net.dunice.mk.rsmtelegrambot.event.BroadcastUsersEvent;
import net.dunice.mk.rsmtelegrambot.handler.MenuGenerator;
import net.dunice.mk.rsmtelegrambot.handler.state.BasicState;
import net.dunice.mk.rsmtelegrambot.handler.state.BasicState.BasicStep;
import net.dunice.mk.rsmtelegrambot.handler.state.MessageBroadcastState;
import net.dunice.mk.rsmtelegrambot.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageBroadcastHandler implements MessageHandler {

    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Map<Long, MessageBroadcastState> states;
    private final MenuGenerator menuGenerator;
    private final Map<Long, BasicState> basicStates;
    private final EnumMap<Menu, ReplyKeyboard> menus;

    @Override
    public SendMessage handle(MessageDto messageDto, Long telegramId) {
        String messageText = messageDto.getText();
        MessageBroadcastState state = states.get(telegramId);
        if (state == null) {
            states.put(telegramId, (state = new MessageBroadcastState()));
        }
        ReplyKeyboard mainMenu = menus.get(GO_TO_MAIN_MENU);

        if (TO_MAIN_MENU.equals(messageText)) {
            return switchToMainMenu(telegramId);
        }

        return switch (state.getStep()) {
            case REQUEST_MESSAGE_TEXT -> {
                state.setStep(VERIFY_MESSAGE_TEXT);
                yield generateSendMessage(telegramId,
                    "Введите текст сообщения, которое хотите отправить всем пользователям.");
            }
            case VERIFY_MESSAGE_TEXT -> {
                state.setStep(CONFIRM_MESSAGE_TEXT);
                String sender = userRepository.findByTelegramId(telegramId).get().getFullName();
                state.setText(messageText + "\n\nОтправлено пользователем: " + sender);
                String formattedMessage = String.format(
                    "Вы хотите отправить следующее сообщение:\n\n" +
                    "---------------\n%s\n---------------\n\nПодтвердите отправку.",
                    state.getText()
                );
                yield generateSendMessage(telegramId, formattedMessage, menus.get(SELECTION_MENU));
            }
            case CONFIRM_MESSAGE_TEXT -> {
                if ("Да".equalsIgnoreCase(messageText)) {
                    broadcastMessage(state.getText());
                    states.remove(telegramId);
                    yield generateSendMessage(telegramId, "Сообщение успешно разослано всем пользователям.",
                        mainMenu);
                } else if ("Нет".equalsIgnoreCase(messageText)) {
                    state.setStep(REQUEST_MESSAGE_TEXT);
                    yield generateSendMessage(telegramId, "Отправка сообщения отменена.", mainMenu);
                } else {
                    yield generateSendMessage(telegramId,
                        "Неверная команда. Отправка сообщения отменена.",
                        mainMenu);
                }
            }
        };
    }

    @Override
    public BasicStep getStep() {
        return SEND_MESSAGE_TO_EVERYBODY;
    }

    private SendMessage switchToMainMenu(Long telegramId) {
        BasicState state = basicStates.get(telegramId);
        states.remove(telegramId);
        state.setStep(IN_MAIN_MENU);
        return menuGenerator.generateRoleSpecificMainMenu(telegramId,
            state.getUser().getUserRole());
    }

    public void broadcastMessage(String text) {
        List<User> users = userRepository.findAll();
        eventPublisher.publishEvent(new BroadcastUsersEvent(text, users));
    }
}
