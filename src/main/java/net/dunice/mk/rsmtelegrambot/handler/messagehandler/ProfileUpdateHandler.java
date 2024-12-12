package net.dunice.mk.rsmtelegrambot.handler.messagehandler;

import static net.dunice.mk.rsmtelegrambot.constant.InteractionState.CHANGE_PROFILE;

import lombok.RequiredArgsConstructor;
import net.dunice.mk.rsmtelegrambot.constant.InteractionState;
import net.dunice.mk.rsmtelegrambot.constant.UserUpdateStep;
import net.dunice.mk.rsmtelegrambot.entity.User;
import net.dunice.mk.rsmtelegrambot.service.UserService;
import net.dunice.mk.rsmtelegrambot.service.UserUpdateState;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class ProfileUpdateHandler implements MessageHandler {

    private final Map<Long, UserUpdateState> userUpdateState = new ConcurrentHashMap<>();
    private final UserService userService;

    @Override
    public SendMessage handleMessage(String message, Long telegramId) {
        UserUpdateState state = userUpdateState.get(telegramId);
        if (state == null) {
            userUpdateState.put(telegramId, (state = new UserUpdateState()));
        }

        if ("Отмена".equalsIgnoreCase(message)) {
            userUpdateState.remove(telegramId);
            return generateSendMessage(telegramId, "Обновление профиля отменено.");
        }

        if ("Готово".equalsIgnoreCase(message)) {
            userUpdateState.remove(telegramId);
            saveUser(state, telegramId);
            return generateSendMessage(telegramId, "Изменения сохранены.");
        }

        String response = switch (state.getStep()) {
            case CONFIRM -> {
                if ("Изменить профиль".equals(message)) {
                    state.setStep(UserUpdateStep.FULL_NAME);
                    yield "Введите ФИО (Можете ввести 'Отмена' для отмены редактирования или" +
                          " 'Готово' для сохранения текущих изменений):";
                } else if ("Нет".equalsIgnoreCase(message)) {
                    userUpdateState.remove(telegramId);
                    yield "Обновление пользователя отменено";
                } else {
                    userUpdateState.remove(telegramId);
                    yield "Неверная команда";
                }
            }
            case FULL_NAME -> {
                state.setFullName(message);
                String[] nameParts = state.getFullName().trim().split("\\s+");
                if (nameParts.length < 3) {
                    userUpdateState.remove(telegramId);
                    yield "Ошибка: ФИО должно содержать минимум 3 слова. Обновление отменено.";
                }
                state.setName(nameParts[1]);
                state.setStep(UserUpdateStep.PHONE_NUMBER);
                yield "Введите номер телефона (Можете ввести 'Отмена' для отмены редактирования или" +
                      " 'Готово' для сохранения текущих изменений):";
            }
            case PHONE_NUMBER -> {
                state.setPhoneNumber(message);
                state.setStep(UserUpdateStep.INFO);
                yield "Введите дополнительное описание (до 255 символов) (Можете ввести 'Отмена' для отмены " +
                      "редактирования или 'Готово' для сохранения текущих изменений):";
            }
            case INFO -> {
                if (message.length() <= 255) {
                    state.setInfo(message);
                    userUpdateState.remove(telegramId);
                    saveUser(state, telegramId);
                    yield "Вы успешно изменили свои данные!";
                } else {
                    yield "Описание слишком длинное. Попробуйте снова.";
                }
            }
        };
        return generateSendMessage(telegramId, response);
    }

    private void saveUser(UserUpdateState state, long telegramId) {
        User user = userService.getUserByTelegramId(telegramId);
        if (state.getFullName() != null) {
            user.setFullName(state.getFullName());
        }
        if (state.getName() != null) {
            user.setName(state.getName());
        }
        if (state.getPhoneNumber() != null) {
            user.setPhoneNumber(state.getPhoneNumber());
        }
        if (state.getInfo() != null) {
            user.setInfo(state.getInfo());
        }
        userService.saveUser(user);
    }

    @Override
    public InteractionState getState() {
        return CHANGE_PROFILE;
    }
}
