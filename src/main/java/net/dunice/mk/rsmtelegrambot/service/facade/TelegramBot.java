package net.dunice.mk.rsmtelegrambot.service.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dunice.mk.rsmtelegrambot.service.RegistrationService;
import net.dunice.mk.rsmtelegrambot.service.UserService;
import net.dunice.mk.rsmtelegrambot.service.util.TelegramAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramBot extends TelegramLongPollingBot implements TelegramAdapter {

    private final UserService userService;
    private final RegistrationService registrationService;
    @Value("${telegram-bot-token}")
    private String botToken;
    @Value("${telegram-bot-name}")
    private String botName;

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Long chatId = update.getMessage().getChatId();
            String text = update.getMessage().getText();
            if ("/start".equalsIgnoreCase(text)) {
                handleStartCommand(chatId);
            } else {
                registrationService.processUserMessage(chatId, text);
            }
        }
    }

    private void handleStartCommand(Long chatId) {
        if (userService.findByTelegramId(chatId).isPresent()) {
            sendMessage(chatId, "Вы уже зарегистрированы!");
        } else {
            registrationService.initiateRegistration(chatId);
        }
    }

    @Override
    public void sendMessage(Long chatId, String message) {
        try {
            execute(SendMessage.builder().chatId(chatId).text(message).build());
        } catch (TelegramApiException e) {
            log.error("Не удалось отправить сообщение", e);
        }
    }

    @Override
    public String getBotUsername() {
        return botName;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

}
