package net.dunice.mk.rsmtelegrambot.service;

import lombok.extern.slf4j.Slf4j;
import net.dunice.mk.rsmtelegrambot.constants.UserRegistrationStep;
import net.dunice.mk.rsmtelegrambot.entity.Role;
import net.dunice.mk.rsmtelegrambot.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class TelegramBot extends TelegramLongPollingBot {

    private static int currentUserCard = 1;

    private final UserService userService;
    private final Map<Long, UserRegistrationState> registrationState = new ConcurrentHashMap<>();

    @Value("${bot.name}")
    private String BOT_NAME;

    @Value("${bot.token}")
    private String BOT_TOKEN;

    public TelegramBot(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            long telegramId = update.getMessage().getFrom().getId();
            String messageText = update.getMessage().getText();

            if (messageText.equalsIgnoreCase("/start")) {
                if (userService.isUserRegistered(telegramId)) {
                    sendMessage(telegramId, "Вы уже зарегистрированы");
                } else {
                    sendMessage(telegramId,
                        "Добро пожаловать! Вы не зарегистрированы, желаете пройти регистрацию? Ответьте 'Да' или 'Нет'.");
                    UserRegistrationState state = new UserRegistrationState();
                    state.setStep(UserRegistrationStep.CONFIRM);
                    registrationState.put(telegramId, state);
                }
            } else {
                handleRegistrationFlow(telegramId, messageText);
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

    private void handleRegistrationFlow(long telegramId, String messageText) {
        UserRegistrationState state = registrationState.getOrDefault(telegramId, new UserRegistrationState());
        switch (state.getStep()) {
            case CONFIRM -> {
                if ("Да".equalsIgnoreCase(messageText)) {
                    state.setStep(UserRegistrationStep.FULL_NAME);
                    sendMessage(telegramId, "Введите ФИО:");
                } else if ("Отменить регистрацию".equalsIgnoreCase(messageText)) {
                    sendMessage(telegramId, "Регистрация отменена.");
                    registrationState.remove(telegramId);
                }
            }
            case FULL_NAME -> {
                state.setFullName(messageText);
                state.setStep(UserRegistrationStep.PHONE_NUMBER);
                sendMessage(telegramId, "Введите номер телефона:");
            }
            case PHONE_NUMBER -> {
                state.setPhoneNumber(messageText);
                state.setStep(UserRegistrationStep.INFO);
                sendMessage(telegramId, "Введите дополнительное описание (до 255 символов):");
            }
            case INFO -> {
                if (messageText.length() <= 255) {
                    state.setInfo(messageText);
                    saveUser(state, telegramId);
                    sendMessage(telegramId, "Вы успешно зарегистрированы!");
                    registrationState.remove(telegramId);
                } else {
                    sendMessage(telegramId, "Описание слишком длинное. Попробуйте снова.");
                }
            }
        }
        registrationState.put(telegramId, state);
    }

    private void saveUser(UserRegistrationState state, long telegramId) {
        User user = new User();
        user.setTelegramId(telegramId);
        user.setFullName(state.getFullName());
        String[] nameParts = state.getFullName().trim().split("\\s+");
        if (nameParts.length < 2) {
            sendMessage(telegramId, "Ошибка: ФИО должно содержать минимум 3 слова. Регистрация отменена.");
            registrationState.remove(telegramId);
            return;
        }
        String firstName = nameParts[1];
        user.setName(firstName);
        user.setUserCard(currentUserCard++);
        user.setPhoneNumber(state.getPhoneNumber());
        user.setInfo(state.getInfo());
        user.setUserRole(Role.USER);
        user.setBirthDate(LocalDate.now()); // Временно
        userService.saveUser(user);
    }

    private void sendMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Не удалось отправить сообщение", e);
        }
    }
}
