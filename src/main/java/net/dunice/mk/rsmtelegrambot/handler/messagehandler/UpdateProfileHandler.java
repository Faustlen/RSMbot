package net.dunice.mk.rsmtelegrambot.handler.messagehandler;

import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.CANCEL;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.TO_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.CANCEL_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.GO_TO_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.CHANGE_PROFILE;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.IN_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.handler.state.stateobject.UpdateProfileState.UpdateProfileStep.VERIFY_USER_INFO;

import lombok.RequiredArgsConstructor;
import net.dunice.mk.rsmtelegrambot.constant.Menu;
import net.dunice.mk.rsmtelegrambot.dto.MessageDto;
import net.dunice.mk.rsmtelegrambot.entity.User;
import net.dunice.mk.rsmtelegrambot.handler.MenuGenerator;
import net.dunice.mk.rsmtelegrambot.handler.state.BasicState;
import net.dunice.mk.rsmtelegrambot.handler.state.stateobject.UpdateProfileState;
import net.dunice.mk.rsmtelegrambot.repository.UserRepository;
import net.dunice.mk.rsmtelegrambot.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class UpdateProfileHandler implements MessageHandler {

    private final Map<Long, BasicState> basicStates;
    private final Map<Long, UpdateProfileState> updateProfileStates;
    private final Map<Menu, ReplyKeyboard> menus;
    private final UserRepository userRepository;
    private final MenuGenerator menuGenerator;

    @Override
    public SendMessage handle(MessageDto messageDto, Long telegramId) {
        String text = messageDto.getText();
        UpdateProfileState state = updateProfileStates.get(telegramId);
        if (state == null) {
            updateProfileStates.put(telegramId, (state = new UpdateProfileState()));
        }

        if (StringUtils.equalsAny(text, CANCEL, TO_MAIN_MENU)) {
            return goToMainMenu(telegramId);
        }

        return switch (state.getStep()) {
            case REQUEST_USER_INFO -> {
                state.setStep(VERIFY_USER_INFO);
                yield generateSendMessage(telegramId,
                    "Введите дополнительное описание о себе (до 255 символов):", menus.get(CANCEL_MENU));
            }
            case VERIFY_USER_INFO -> {
                if (text.length() <= 255) {
                    userRepository.updateInfoById(telegramId, text);
                    yield generateSendMessage(telegramId, "Вы успешно изменили свои данные!", menus.get(GO_TO_MAIN_MENU));
                } else {
                    yield generateSendMessage(telegramId, "Описание слишком длинное. Попробуйте снова:", menus.get(CANCEL_MENU));
                }
            }
        };
    }

    @Override
    public BasicState getState() {
        return CHANGE_PROFILE;
    }

    private SendMessage goToMainMenu(Long telegramId) {
        updateProfileStates.remove(telegramId);
        basicStates.put(telegramId, IN_MAIN_MENU);
        return menuGenerator.generateRoleSpecificMainMenu(telegramId,
            userRepository.findByTelegramId(telegramId).get().getUserRole());
    }
}
