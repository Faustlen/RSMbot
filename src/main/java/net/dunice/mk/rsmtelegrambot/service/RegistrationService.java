package net.dunice.mk.rsmtelegrambot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dunice.mk.rsmtelegrambot.constants.RegistrationState;
import net.dunice.mk.rsmtelegrambot.entity.UserEntity;
import net.dunice.mk.rsmtelegrambot.service.util.TelegramAdapter;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final TelegramAdapter telegramAdapter;

    private final UserService userService;

    private final Map<Long, RegistrationState> userStates = new ConcurrentHashMap<>();

    private UserEntity user;

    public void initiateRegistration(Long chatId) {
        try {

            userStates.put(chatId, RegistrationState.AWAITING_CONFIRMATION);
            telegramAdapter.sendMessage(chatId, "Вы не зарегистрированы. Хотите пройти регистрацию?");
        } catch (TelegramApiException e) {
            log.error("Невозможно инициировать регистрацию из-за ошибки", e);
        }
    }

    public void processUserMessage(Long chatId, String message) {
        RegistrationState state = userStates.get(chatId);
        if (state == null) {
            try {
                telegramAdapter.sendMessage(chatId, "Введите /start или нажмите на кнопку для начала работы.");
            } catch (TelegramApiException e) {
                log.error("Произошла ошибка при отправке стартового сообщения");
            }
        }
        switch (state) {
            case AWAITING_CONFIRMATION -> handleConfirmation(chatId, message);
            case AWAITING_FULL_NAME -> handleFullName(chatId, message);
            case AWAITING_PHONE_NUMBER -> handlePhoneNumber(chatId, message);
            case AWAITING_PROFILE_DESCRIPTION -> handleProfileDescription(chatId, message);
        }
    }

    private void handleConfirmation(Long chatId, String message) {
        if ("Да".equalsIgnoreCase(message)) {
            userStates.put(chatId, RegistrationState.AWAITING_FULL_NAME);
            try {
                telegramAdapter.sendMessage(chatId, "Введите ваше ФИО. Полностью и без сокращений.");
            } catch (TelegramApiException e) {
                log.error("Не удалось отправить сообщение с запросом ФИО", e);
            }
        } else if ("Нет".equalsIgnoreCase(message)) {
            userStates.remove(chatId);
            try {


                telegramAdapter.sendMessage(chatId, "Регистрация отменена.");
            } catch (TelegramApiException e) {
                log.error("Не удалось отправить сообщение об отмене регистрации");
            }
        } else {
            try {


                telegramAdapter.sendMessage(chatId, "Пожалуйста, ответьте 'Да' или 'Нет'.");
            } catch (TelegramApiException e) {
                log.error("Не удалось отправить сообщение с повторным запросом подтверждения");
            }
        }
    }

    private void handleFullName(Long chatId, String message) {
        String fullName = message.trim();
        String name = extractName(fullName);
        user.setTelegramId(chatId);
        user.setFullName(message.toLowerCase());
        user.setFirstName(name);
        userStates.put(chatId, RegistrationState.AWAITING_PHONE_NUMBER);
        try {
            telegramAdapter.sendMessage(chatId, "Введите ваш номер телефона.");
        } catch (TelegramApiException e) {
            log.error("Не удалось перейти на этап запроса номера телефона");
        }
    }

    private String extractName(String fullName) {
        String[] parts = fullName.split("\\s+");
        if (parts.length > 1) {
            return parts[1];
        }
        return parts[0];
    }

    private void handlePhoneNumber(Long chatId, String message) {
        user.setPhoneNumber(message);
        userStates.put(chatId, RegistrationState.AWAITING_PROFILE_DESCRIPTION);
        try {
            telegramAdapter.sendMessage(chatId, "Введите вашу дату рождения (ДД.ММ.ГГГГ).");
        } catch (TelegramApiException e) {
            log.error("Не удалось перейти на этап запроса даты рождения");
        }
    }

    private void handleProfileDescription(Long chatId, String message) {
        user.setInfo(message);
        userService.save(user);
        userStates.remove(chatId);
        try {
            telegramAdapter.sendMessage(chatId, "Вы успешно зарегистрированы! Спасибо!");
        } catch (TelegramApiException e) {
            log.error("Не удалось завершить регистрацию");
        }
    }
}
