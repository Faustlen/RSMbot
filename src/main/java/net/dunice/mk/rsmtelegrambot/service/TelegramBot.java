package net.dunice.mk.rsmtelegrambot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dunice.mk.rsmtelegrambot.constant.Command;
import net.dunice.mk.rsmtelegrambot.handler.CommandHandler;
import net.dunice.mk.rsmtelegrambot.handler.messagehandler.MessageHandler;
import net.dunice.mk.rsmtelegrambot.handler.state.BasicState;
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

    private final CommandHandler commandHandler;
    private final Map<Long, BasicState> basicStates;
    private final Set<MessageHandler> messageHandlers;
    private final Map<Long, Integer> lastBotMessageIds = new ConcurrentHashMap<>();
    private final List<Map<Long, ?>> allStatesMap;

    @Value("${bot.name}")
    private String BOT_NAME;

    @Value("${bot.token}")
    private String BOT_TOKEN;

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Message message = update.getMessage();
            Long telegramId = message.getFrom().getId();
            Integer userMessageId = message.getMessageId();
            Integer botMessageId = lastBotMessageIds.remove(telegramId);
            String text = message.getText();
            deletePreviousMessages(telegramId, userMessageId, botMessageId);
            BasicState currentState = basicStates.get(telegramId);
            if (Command.isValidCommand(text) || currentState == null) {
                basicStates.remove(telegramId);
                allStatesMap.forEach(map -> map.remove(telegramId));
                sendMessage(commandHandler.handle(text, telegramId));
            } else {
                Optional<MessageHandler> handler = getMessageHandlerForState(currentState);
                sendMessage(handler.isPresent()
                    ? handler.get().handle(text, telegramId)
                    : generateHandlerNotFoundMessage(telegramId));
            }
        } else if (update.hasCallbackQuery()) {
            Long telegramId = update.getCallbackQuery().getFrom().getId();
            String data = update.getCallbackQuery().getData();
            deleteMessage(telegramId, lastBotMessageIds.remove(telegramId));
            BasicState currentState = basicStates.get(telegramId);
            Optional<MessageHandler> handler = getMessageHandlerForState(currentState);
            sendMessage(handler.isPresent()
                ? handler.get().handle(data, telegramId)
                : generateHandlerNotFoundMessage(telegramId));
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

    private Optional<MessageHandler> getMessageHandlerForState(BasicState state) {
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

    private void deletePreviousMessages(Long chatId, Integer userMessageId, Integer botMessageId) {
        if (botMessageId != null) {
            deleteMessage(chatId, botMessageId);
        }
        deleteMessage(chatId, userMessageId);
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