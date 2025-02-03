package net.dunice.mk.rsmtelegrambot.service;

import static net.dunice.mk.rsmtelegrambot.constant.Command.START;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dunice.mk.rsmtelegrambot.constant.Command;
import net.dunice.mk.rsmtelegrambot.dto.MessageDto;
import net.dunice.mk.rsmtelegrambot.handler.CommandHandler;
import net.dunice.mk.rsmtelegrambot.handler.MessageGenerator;
import net.dunice.mk.rsmtelegrambot.handler.messagehandler.BroadcastResponseHandler;
import net.dunice.mk.rsmtelegrambot.handler.messagehandler.MessageHandler;
import net.dunice.mk.rsmtelegrambot.handler.state.BasicState;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramBot extends TelegramLongPollingBot implements MessageGenerator {

    private final CommandHandler commandHandler;
    private final BroadcastResponseHandler broadcastResponseHandler;
    private final Map<Long, BasicState> basicStates;
    private final Set<MessageHandler> messageHandlers;
    private final Map<Long, Integer> lastBotMessageIdMap;
    private final List<Map<Long, ?>> allStatesMap;

    @Value("${bot.name}")
    private String BOT_NAME;

    @Value("${bot.token}")
    private String BOT_TOKEN;

    @Override
    public String getBotUsername() {
        return BOT_NAME;
    }

    @Override
    public String getBotToken() {
        return BOT_TOKEN;
    }

    @Override
    public void onUpdateReceived(Update update) {
        MessageDto messageDto = new MessageDto();
        if (update.hasMessage()) {
            Message message = update.getMessage();
            Long telegramId = message.getFrom().getId();
            Integer userMessageId = message.getMessageId();
            Integer botMessageId = lastBotMessageIdMap.remove(telegramId);
            //deleteMessage(telegramId, botMessageId);
            String text = message.getText();
            BasicState currentState = basicStates.get(telegramId);
            if (currentState == null) {
                text = START.getStringValue();
            }
            if (Command.isValidCommand(text)) {
                basicStates.remove(telegramId);
                allStatesMap.forEach(map -> map.remove(telegramId));
                messageDto.setText(text);
                sendMessage(commandHandler.handle(messageDto, telegramId));
            } else {
                byte[] image = downloadImageIfPresent(message);
                Optional<MessageHandler> handler = getMessageHandlerForState(currentState);
                messageDto.setText(text);
                messageDto.setImage(image);
                sendMessage(handler.isPresent()
                    ? handler.get().handle(messageDto, telegramId)
                    : generateSendMessage(telegramId, "Обработчик команды не найден"));
            }
            deletePreviousMessages(telegramId, userMessageId, botMessageId);
        } else if (update.hasCallbackQuery()) {
            Long telegramId = update.getCallbackQuery().getFrom().getId();
            Integer botMessageId = lastBotMessageIdMap.remove(telegramId);
            String text = update.getCallbackQuery().getData();
            if (text.startsWith("broadcast_")) {
                messageDto.setText(text.replace("broadcast_", ""));
                sendMessage(broadcastResponseHandler.handle(messageDto, telegramId));
                deleteMessage(telegramId, update.getCallbackQuery().getMessage().getMessageId());
            } else {
                BasicState currentState = basicStates.get(telegramId);
                Optional<MessageHandler> handler = getMessageHandlerForState(currentState);
                messageDto.setText(text);
                sendMessage(handler.isPresent()
                    ? handler.get().handle(messageDto, telegramId)
                    : generateSendMessage(telegramId, "Обработчик команды не найден"));
                deleteMessage(telegramId, botMessageId);
            }
        }
    }

    public void sendMessage(PartialBotApiMethod<Message> message) {
        try {
            Message sentMessage = switch (message) {
                case SendMessage sendMessage -> execute(sendMessage);
                case SendPhoto sendPhoto -> execute(sendPhoto);
                default -> throw new IllegalStateException("Unexpected value: " + message);
            };
            lastBotMessageIdMap.put(sentMessage.getChatId(), sentMessage.getMessageId());
        } catch (TelegramApiException e) {
            log.error("Не удалось отправить сообщение", e);
        }
    }

    public void sendNoDeleteMessage(PartialBotApiMethod<Message> message) {
        try {
            Message sentMessage = switch (message) {
                case SendMessage sendMessage -> execute(sendMessage);
                case SendPhoto sendPhoto -> execute(sendPhoto);
                default -> throw new IllegalStateException("Unexpected value: " + message);
            };
        } catch (TelegramApiException e) {
            log.error("Не удалось отправить сообщение", e);
        }
    }

    private Optional<MessageHandler> getMessageHandlerForState(BasicState state) {
        return messageHandlers.stream()
            .filter(handler -> handler.getStep() == state.getStep())
            .findFirst();
    }

    private void deleteMessage(Long chatId, Integer messageId) {
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

    private byte[] downloadImageIfPresent(Message message) {
        byte[] image = null;
        String imageID = message.hasPhoto() ? message.getPhoto().getLast().getFileId()
            : message.hasDocument() ? message.getDocument().getFileId() : null;
        try {
            if (imageID != null) {
                InputStream inputStream = downloadFileAsStream(execute(new GetFile(imageID)));
                image = inputStream.readAllBytes();
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return image;
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