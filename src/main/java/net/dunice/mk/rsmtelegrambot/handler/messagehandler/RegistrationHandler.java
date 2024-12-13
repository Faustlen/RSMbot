package net.dunice.mk.rsmtelegrambot.handler.messagehandler;

import static net.dunice.mk.rsmtelegrambot.constant.Menu.SELECTION_MENU;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.REGISTRATION;
import static net.dunice.mk.rsmtelegrambot.handler.state.stateobject.step.RegistrationStep.FULL_NAME;
import static net.dunice.mk.rsmtelegrambot.handler.state.stateobject.step.RegistrationStep.INFO;
import static net.dunice.mk.rsmtelegrambot.handler.state.stateobject.step.RegistrationStep.PHONE_NUMBER;

import lombok.RequiredArgsConstructor;
import net.dunice.mk.rsmtelegrambot.constant.Menu;
import net.dunice.mk.rsmtelegrambot.entity.Role;
import net.dunice.mk.rsmtelegrambot.entity.User;
import net.dunice.mk.rsmtelegrambot.handler.state.BasicState;
import net.dunice.mk.rsmtelegrambot.handler.state.stateobject.RegistrationState;
import net.dunice.mk.rsmtelegrambot.service.UserService;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RegistrationHandler implements MessageHandler {
    private final Map<Long, RegistrationState> registrationStates;
    private final Map<Long, BasicState> states;
    private static int currentUserCard = 1;
    private final UserService userService;
    private final EnumMap<Menu, ReplyKeyboardMarkup> menus;


    @Override
    public SendMessage handle(String message, Long telegramId) {
        if (states.get(telegramId) == null) {
            states.put(telegramId, REGISTRATION);
            return generateSendMessage(telegramId,
                "Добро пожаловать! Вы не зарегистрированы, желаете пройти регистрацию? Ответьте 'Да' или 'Нет'.",
                menus.get(SELECTION_MENU));
        }

        RegistrationState state = registrationStates.get(telegramId);
        if (state == null) {
            registrationStates.put(telegramId, (state = new RegistrationState()));
        }
        String response = switch (state.getStep()) {
            case CONFIRM -> {
                if ("Да".equalsIgnoreCase(message)) {
                    state.setStep(FULL_NAME);
                    yield "Введите ФИО:";
                } else if ("Нет".equalsIgnoreCase(message)) {
                    cleanStates(telegramId);
                    yield "Регистрация отменена.";
                } else {
                    cleanStates(telegramId);
                    yield "Неверная команда";
                }
            }
            case FULL_NAME -> {
                state.setFullName(message);
                String[] nameParts = state.getFullName().trim().split("\\s+");
                if (nameParts.length < 3) {
                    cleanStates(telegramId);
                    yield "Ошибка: ФИО должно содержать минимум 3 слова. Регистрация отменена.";
                }
                state.setName(nameParts[1]);
                state.setStep(PHONE_NUMBER);
                yield "Введите номер телефона:";
            }
            case PHONE_NUMBER -> {
                state.setPhoneNumber(message);
                state.setStep(INFO);
                yield "Введите дополнительное описание (до 255 символов):";
            }
            case INFO -> {
                if (message.length() <= 255) {
                    state.setInfo(message);
                    saveUser(state, telegramId);
                    cleanStates(telegramId);
                    yield "Вы успешно зарегистрированы!";
                } else {
                    cleanStates(telegramId);
                    yield "Описание слишком длинное. Попробуйте снова.";
                }
            }
        };
        return generateSendMessage(telegramId, response, null);
    }

    private void saveUser(RegistrationState state, long telegramId) {
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

    private void cleanStates(long telegramId) {
        registrationStates.remove(telegramId);
        states.remove(telegramId);
    }

    @Override
    public BasicState getState() {
        return REGISTRATION;
    }
}
