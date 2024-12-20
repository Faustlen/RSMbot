package net.dunice.mk.rsmtelegrambot.handler.messagehandler;

import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.CHANGE_PROFILE;
import static net.dunice.mk.rsmtelegrambot.handler.state.stateobject.UpdateProfileState.UpdateProfileStep.FULL_NAME;
import static net.dunice.mk.rsmtelegrambot.handler.state.stateobject.UpdateProfileState.UpdateProfileStep.INFO;
import static net.dunice.mk.rsmtelegrambot.handler.state.stateobject.UpdateProfileState.UpdateProfileStep.PHONE_NUMBER;

import lombok.RequiredArgsConstructor;
import net.dunice.mk.rsmtelegrambot.dto.MessageDto;
import net.dunice.mk.rsmtelegrambot.entity.User;
import net.dunice.mk.rsmtelegrambot.handler.state.BasicState;
import net.dunice.mk.rsmtelegrambot.handler.state.stateobject.UpdateProfileState;
import net.dunice.mk.rsmtelegrambot.service.UserService;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class UpdateProfileHandler implements MessageHandler {

    private final Map<Long, BasicState> basicStates;
    private final Map<Long, UpdateProfileState> updateProfileStates;
    private final UserService userService;

    @Override
    public SendMessage handle(MessageDto messageDto, Long telegramId) {
        String text = messageDto.getText();
        UpdateProfileState state = updateProfileStates.get(telegramId);
        if (state == null) {
            updateProfileStates.put(telegramId, (state = new UpdateProfileState()));
        }

        if ("Отмена".equalsIgnoreCase(text)) {
            cleanStates(telegramId);
            return generateSendMessage(telegramId, "Обновление профиля отменено.");
        }

        if ("Готово".equalsIgnoreCase(text)) {
            cleanStates(telegramId);
            saveUser(state, telegramId);
            return generateSendMessage(telegramId, "Изменения сохранены.");
        }

        String response = switch (state.getStep()) {
            case CONFIRM -> {
                if ("Изменить профиль".equals(text)) {
                    state.setStep(FULL_NAME);
                    yield "Введите ФИО (Можете ввести 'Отмена' для отмены редактирования или" +
                          " 'Готово' для сохранения текущих изменений):";
                } else if ("Нет".equalsIgnoreCase(text)) {
                    cleanStates(telegramId);
                    yield "Обновление пользователя отменено";
                } else {
                    cleanStates(telegramId);
                    yield "Неверная команда";
                }
            }
            case FULL_NAME -> {
                state.setFullName(text);
                String[] nameParts = state.getFullName().trim().split("\\s+");
                if (nameParts.length < 3) {
                    cleanStates(telegramId);
                    yield "Ошибка: ФИО должно содержать минимум 3 слова. Обновление отменено.";
                }
                state.setName(nameParts[1]);
                state.setStep(PHONE_NUMBER);
                yield "Введите номер телефона (Можете ввести 'Отмена' для отмены редактирования или" +
                      " 'Готово' для сохранения текущих изменений):";
            }
            case PHONE_NUMBER -> {
                state.setPhoneNumber(text);
                state.setStep(INFO);
                yield "Введите дополнительное описание (до 255 символов) (Можете ввести 'Отмена' для отмены " +
                      "редактирования или 'Готово' для сохранения текущих изменений):";
            }
            case INFO -> {
                if (text.length() <= 255) {
                    state.setInfo(text);
                    cleanStates(telegramId);
                    saveUser(state, telegramId);
                    yield "Вы успешно изменили свои данные!";
                } else {
                    yield "Описание слишком длинное. Попробуйте снова.";
                }
            }
        };
        return generateSendMessage(telegramId, response);
    }

    private void saveUser(UpdateProfileState state, long telegramId) {
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
    public BasicState getState() {
        return CHANGE_PROFILE;
    }

    private void cleanStates(long telegramId) {
        updateProfileStates.remove(telegramId);
        basicStates.remove(telegramId);
    }
}
