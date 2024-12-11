package net.dunice.mk.rsmtelegrambot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dunice.mk.rsmtelegrambot.constants.InteractionState;
import net.dunice.mk.rsmtelegrambot.entity.Role;
import net.dunice.mk.rsmtelegrambot.entity.User;
import net.dunice.mk.rsmtelegrambot.handler.clickhandler.ClickHandler;
import net.dunice.mk.rsmtelegrambot.handler.messagehandler.MessageHandler;
import net.dunice.mk.rsmtelegrambot.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static net.dunice.mk.rsmtelegrambot.constants.InteractionState.*;
import static net.dunice.mk.rsmtelegrambot.entity.Role.ADMIN;
import static net.dunice.mk.rsmtelegrambot.entity.Role.SUPER_USER;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramBot extends TelegramLongPollingBot {

    private final UserService userService;
    private final Map<Long, InteractionState> interactionStates = new ConcurrentHashMap<>();
    private final Set<MessageHandler> messageHandlers;
    private final Set<ClickHandler> clickHandlers;
    private final EnumMap<InteractionState, ReplyKeyboardMarkup> menus;
    private final UserRepository userRepository;
    private final Map<Long, Integer> lastBotMessageIds = new ConcurrentHashMap<>();

    @Value("${bot.name}")
    private String BOT_NAME;

    @Value("${bot.token}")
    private String BOT_TOKEN;

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            handleTextMessage(update.getMessage());
        }
        else if (update.hasCallbackQuery()) {
            handleCallbackQuery(update.getCallbackQuery());
        }
    }

    @Override
    public String getBotUsername() {
        return BOT_NAME;
    }

    @Override
    public String getBotToken() {
        return BOT_TOKEN;
    }

    private void handleTextMessage(Message message) {
        long telegramId = message.getFrom().getId();
        String text = message.getText();

        Integer lastBotMessageId = lastBotMessageIds.remove(telegramId);
        if (lastBotMessageId != null) {
            deleteReplyMessage(telegramId, lastBotMessageId);
        }
        deleteReplyMessage(telegramId, message.getMessageId());

        if (text.equalsIgnoreCase("/start")) {
            handleStartCommand(telegramId);
        }
        else if (text.equalsIgnoreCase("/set_superuser")) {
            handleBecomeSuperuserCommand(telegramId);
        }
        else {
            InteractionState currentState = interactionStates.get(telegramId);
            Optional<MessageHandler> handler = getMessageHandlerForState(currentState);
            sendMessage(telegramId, handler.isPresent()
                    ? handler.get().handleMessage(text, telegramId)
                    : "Обработчик команды не найден");
        }
    }

    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        long telegramId = callbackQuery.getMessage().getChatId();
        String data = callbackQuery.getData();
        InteractionState currentState = interactionStates.get(telegramId);

        deleteButtons(callbackQuery);

        Optional<ClickHandler> handler = getClickHandlerForState(currentState);
        sendMessage(telegramId, handler.isPresent()
                ? handler.get().handleClick(data, telegramId)
                : "Обработчик команды не найден");
    }

    private void handleStartCommand(long telegramId) {
        Optional<User> user = userRepository.findByTelegramId(telegramId);
        if (user.isPresent()) {
            Role role = user.get().getUserRole();
            InteractionState state = (role == SUPER_USER ? SUPER_USER_MAIN_MENU
                    : role == ADMIN ? ADMIN_MAIN_MENU
                    : USER_MAIN_MENU);
            interactionStates.put(telegramId, state);
            sendMenu(telegramId,
                    "Добро пожаловать! Выберите действие:",
                    menus.get(state));
        }
        else requireRegistration(telegramId);
    }

    private void handleBecomeSuperuserCommand(long telegramId) {
        Optional<User> user = userRepository.findByTelegramId(telegramId);
        if (user.isPresent()) {
            user.get().setUserRole(SUPER_USER);
            userRepository.save(user.get());
            sendMessage(telegramId, "Вы авторизованы как супер пользователь");
        }
        else {
            requireRegistration(telegramId);
        }
    }

    private Optional<MessageHandler> getMessageHandlerForState(InteractionState state) {
        return messageHandlers.stream()
                .filter(handler -> handler.getState() == state)
                .findFirst();
    }

    private Optional<ClickHandler> getClickHandlerForState(InteractionState state) {
        return clickHandlers.stream()
                .filter(handler -> handler.getState() == state)
                .findFirst();
    }

    private void sendMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        try {
            Message sentMessage = execute(message);
            lastBotMessageIds.put(chatId, sentMessage.getMessageId());
        } catch (TelegramApiException e) {
            log.error("Не удалось отправить сообщение", e);
        }
    }

    private void sendMenu(long chatId, String text, ReplyKeyboardMarkup keyboardMarkup) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setReplyMarkup(keyboardMarkup);
        try {
            Message sentMessage = execute(message);
            lastBotMessageIds.put(chatId, sentMessage.getMessageId());
        } catch (TelegramApiException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void deleteButtons(CallbackQuery callbackQuery) {
        Long telegramId = callbackQuery.getMessage().getChatId();
        Integer messageId = callbackQuery.getMessage().getMessageId();
        DeleteMessage deleteMessage = new DeleteMessage();
        deleteMessage.setChatId(telegramId);
        deleteMessage.setMessageId(messageId);
        try {
            execute(deleteMessage);
        } catch (TelegramApiException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void deleteReplyMessage(long telegramId, int messageId) {
        DeleteMessage deleteMessage = new DeleteMessage();
        deleteMessage.setChatId(telegramId);
        deleteMessage.setMessageId(messageId);
        try {
            execute(deleteMessage);
        } catch (TelegramApiException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void requireRegistration(long telegramId) {
        interactionStates.put(telegramId, REGISTRATION);
        sendMenu(telegramId,
                "Добро пожаловать! Вы не зарегистрированы, желаете пройти регистрацию? Ответьте 'Да' или 'Нет'.",
                menus.get(REGISTRATION));
    }

    public void setBotCommands() {
        List<BotCommand> commands = List.of(
                new BotCommand("/start", "Запустить бота")
        );

        SetMyCommands setMyCommands = new SetMyCommands();
        setMyCommands.setCommands(commands);

        try {
            execute(setMyCommands);
            log.info("Команды бота успешно настроены");
        } catch (TelegramApiException e) {
            log.error("Ошибка при настройке команд: {}", e.getMessage(), e);
        }
    }
}