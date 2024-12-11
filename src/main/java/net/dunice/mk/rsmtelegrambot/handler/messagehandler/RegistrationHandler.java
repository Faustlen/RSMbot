package net.dunice.mk.rsmtelegrambot.handler.messagehandler;

import lombok.RequiredArgsConstructor;
import net.dunice.mk.rsmtelegrambot.constant.InteractionState;
import net.dunice.mk.rsmtelegrambot.constant.UserRegistrationStep;
import net.dunice.mk.rsmtelegrambot.entity.Role;
import net.dunice.mk.rsmtelegrambot.entity.User;
import net.dunice.mk.rsmtelegrambot.service.UserRegistrationState;
import net.dunice.mk.rsmtelegrambot.service.UserService;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static net.dunice.mk.rsmtelegrambot.constant.InteractionState.REGISTRATION;

@Service
@RequiredArgsConstructor
public class RegistrationHandler implements MessageHandler {
    private final Map<Long, UserRegistrationState> registrationState = new ConcurrentHashMap<>();
    private final Map<Long, InteractionState> interactionStates;
    private static int currentUserCard = 1;
    private final UserService userService;


    @Override
    public SendMessage handleMessage(String message, Long telegramId) {
        UserRegistrationState state = registrationState.get(telegramId);
        if (state == null) {
            registrationState.put(telegramId, (state = new UserRegistrationState()) );
        }
        String response = switch (state.getStep()) {
            case CONFIRM -> {
                if ("Да".equalsIgnoreCase(message)) {
                    state.setStep(UserRegistrationStep.FULL_NAME);
                    yield "Введите ФИО:";
                }
                else if ("Нет".equalsIgnoreCase(message)) {
                    cleanRegistrationStates(telegramId);
                    yield  "Регистрация отменена.";
                }
                else {
                    cleanRegistrationStates(telegramId);
                    yield "Неверная команда";
                }
            }
            case FULL_NAME -> {
                state.setFullName(message);
                String[] nameParts = state.getFullName().trim().split("\\s+");
                if (nameParts.length < 3) {
                    cleanRegistrationStates(telegramId);
                    yield "Ошибка: ФИО должно содержать минимум 3 слова. Регистрация отменена.";
                }
                state.setName(nameParts[1]);
                state.setStep(UserRegistrationStep.PHONE_NUMBER);
                yield "Введите номер телефона:";
            }
            case PHONE_NUMBER -> {
                state.setPhoneNumber(message);
                state.setStep(UserRegistrationStep.INFO);
                yield "Введите дополнительное описание (до 255 символов):";
            }
            case INFO -> {
                if (message.length() <= 255) {
                    state.setInfo(message);
                    saveUser(state, telegramId);
                    cleanRegistrationStates(telegramId);
                    yield "Вы успешно зарегистрированы!";
                }
                else {
                    cleanRegistrationStates(telegramId);
                    yield "Описание слишком длинное. Попробуйте снова.";
                }
            }
        };
        return generateSendMessage(telegramId, response, null);
    }

    private void saveUser(UserRegistrationState state, long telegramId) {
        User user = new User();
        user.setTelegramId(telegramId);
        user.setFullName(state.getFullName());
        user.setName(state.getName());
        user.setUserCard(currentUserCard++);
        user.setPhoneNumber(state.getPhoneNumber());
        user.setInfo(state.getInfo());
        user.setUserRole(Role.USER);
        user.setBirthDate(LocalDate.now()); // Временно
        userService.saveUser(user);
    }

    private void cleanRegistrationStates(long telegramId) {
        registrationState.remove(telegramId);
        interactionStates.remove(telegramId);
    }

    @Override
    public InteractionState getState() {
        return REGISTRATION;
    }
}
