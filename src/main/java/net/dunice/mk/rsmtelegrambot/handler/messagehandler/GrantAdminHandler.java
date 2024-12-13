package net.dunice.mk.rsmtelegrambot.handler.messagehandler;

import lombok.RequiredArgsConstructor;
import net.dunice.mk.rsmtelegrambot.handler.state.BasicState;
import net.dunice.mk.rsmtelegrambot.constant.Menu;
import net.dunice.mk.rsmtelegrambot.entity.Role;
import net.dunice.mk.rsmtelegrambot.entity.User;
import net.dunice.mk.rsmtelegrambot.handler.state.stateobject.GrantAdminState;
import net.dunice.mk.rsmtelegrambot.service.UserService;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;

import java.util.EnumMap;
import java.util.Map;

import static net.dunice.mk.rsmtelegrambot.handler.state.stateobject.step.GrantAdminStep.CONFIRM;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.GRANT_ADMIN;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.SELECTION_MENU;

@Service
@RequiredArgsConstructor
public class GrantAdminHandler implements MessageHandler {

    private final UserService userService;
    private final Map<Long, GrantAdminState> grantAdminStates;
    private final Map<Long, BasicState> states;
    private final EnumMap<Menu, ReplyKeyboardMarkup> menus;

    @Override
    public SendMessage handle(String message, Long telegramId) {
        states.put(telegramId, GRANT_ADMIN);

        GrantAdminState state = grantAdminStates.get(telegramId);
        if (state == null) {
            state = new GrantAdminState();
            grantAdminStates.put(telegramId, state);
        }

        return switch (state.getStep()) {
            case USER_ID -> {
                try {
                    Long targetUserId = Long.parseLong(message);
                    User targetUser = userService.getUserByTelegramId(targetUserId);
                    if (targetUser != null) {
                        state.setTargetUser(targetUser);
                        state.setStep(CONFIRM);
                        yield generateSendMessage(telegramId,
                            String.format("Хотите дать права администратора пользователю '%s'?",
                                targetUser.getFullName()),
                            menus.get(SELECTION_MENU));
                    } else {
                        cleanStates(telegramId);
                        yield generateSendMessage(telegramId, "Пользователь с таким ID не найден. Попробуйте снова.");
                    }
                } catch (NumberFormatException e) {
                    cleanStates(telegramId);
                    yield generateSendMessage(telegramId,
                        "Ошибка: введённое значение не является корректным ID пользователя.");
                }
            }
            case CONFIRM -> {
                if ("Да".equalsIgnoreCase(message)) {
                    User targetUser = state.getTargetUser();
                    targetUser.setUserRole(Role.ADMIN);
                    userService.saveUser(targetUser);
                    cleanStates(telegramId);
                    yield generateSendMessage(telegramId,
                        String.format("Пользователю '%s' даны права администратора.", targetUser.getFullName()));
                } else if ("Нет".equalsIgnoreCase(message)) {
                    cleanStates(telegramId);
                    yield generateSendMessage(telegramId, "Назначение роли отменено.");
                } else {
                    cleanStates(telegramId);
                    yield generateSendMessage(telegramId, "Неверная команда. Назначение роли отменено.");
                }
            }
        };
    }

    @Override
    public BasicState getState() {
        return GRANT_ADMIN;
    }

    private void cleanStates(long telegramId) {
        grantAdminStates.remove(telegramId);
        states.remove(telegramId);
    }
}
