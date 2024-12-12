package net.dunice.mk.rsmtelegrambot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dunice.mk.rsmtelegrambot.constant.InteractionState;
import net.dunice.mk.rsmtelegrambot.handler.StartCommandHandler;
import net.dunice.mk.rsmtelegrambot.handler.SustartCommandHandler;
import net.dunice.mk.rsmtelegrambot.handler.messagehandler.MessageHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramBot extends TelegramLongPollingBot {

    private final StartCommandHandler startCommandHandler;
    private final SustartCommandHandler sustartCommandHandler;
    private final Map<Long, InteractionState> interactionStates;
    private final Set<MessageHandler> messageHandlers;
    private final Map<Long, Integer> lastBotMessageIds = new ConcurrentHashMap<>();

    @Value("${bot.name}")
    private String BOT_NAME;

    @Value("${bot.token}")
    private String BOT_TOKEN;

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Message message = update.getMessage();
            long telegramId = message.getFrom().getId();
            String text = message.getText();
            InteractionState currentState = interactionStates.get(telegramId);

            deletePreviousMessages(message);

            if (text.equalsIgnoreCase("/start") || currentState == null) {
                sendMessage(startCommandHandler.handleMessage(text, telegramId));
            } else if (text.equalsIgnoreCase("/sustart")) {
                sendMessage(sustartCommandHandler.handleMessage(text, telegramId));
            } else {
                Optional<MessageHandler> handler = getMessageHandlerForState(currentState);
                sendMessage(handler.isPresent()
                    ? handler.get().handleMessage(text, telegramId)
                    : generateHandlerNotFoundMessage(telegramId));
            }
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

    private Optional<MessageHandler> getMessageHandlerForState(InteractionState state) {
        return messageHandlers.stream()
            .filter(handler -> handler.getState() == state)
            .findFirst();
    }

    private void sendMessage(SendMessage message) {
        try {
            Message sentMessage = execute(message);
            lastBotMessageIds.put(sentMessage.getChatId(), sentMessage.getMessageId());
        } catch (TelegramApiException e) {
            log.error("Не удалось отправить сообщение", e);
        }
    }

    private void deleteMessage(long chatId, int messageId) {
        DeleteMessage deleteMessage = new DeleteMessage();
        deleteMessage.setChatId(chatId);
        deleteMessage.setMessageId(messageId);
        try {
            execute(deleteMessage);
        } catch (TelegramApiException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void deletePreviousMessages(Message message) {
        long chatId = message.getChatId();
        int messageId = message.getMessageId();
        Integer lastBotMessageId = lastBotMessageIds.remove(chatId);
        if (lastBotMessageId != null) {
            deleteMessage(chatId, lastBotMessageId);
        }
        deleteMessage(chatId, messageId);
    }

    private SendMessage generateHandlerNotFoundMessage(long telegramId) {
        SendMessage message = new SendMessage();
        message.setChatId(telegramId);
        message.setText("Обработчик команды не найден");
        return message;
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