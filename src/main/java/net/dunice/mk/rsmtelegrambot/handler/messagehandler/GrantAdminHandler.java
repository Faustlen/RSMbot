package net.dunice.mk.rsmtelegrambot.handler.messagehandler;

import static net.dunice.mk.rsmtelegrambot.constant.GrantAdminStep.CONFIRM;
import static net.dunice.mk.rsmtelegrambot.constant.InteractionState.GRANT_ADMIN;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.SELECTION_MENU;

import lombok.RequiredArgsConstructor;
import net.dunice.mk.rsmtelegrambot.constant.InteractionState;
import net.dunice.mk.rsmtelegrambot.constant.Menu;
import net.dunice.mk.rsmtelegrambot.entity.Role;
import net.dunice.mk.rsmtelegrambot.entity.User;
import net.dunice.mk.rsmtelegrambot.service.GrantAdminState;
import net.dunice.mk.rsmtelegrambot.service.UserService;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class GrantAdminHandler implements MessageHandler {

    private final UserService userService;
    private final Map<Long, GrantAdminState> grantAdminStates = new ConcurrentHashMap<>();
    private final Map<Long, InteractionState> interactionStates;
    private final EnumMap<Menu, ReplyKeyboardMarkup> menus;

    @Override
    public SendMessage handleMessage(String message, Long telegramId) {
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
                        grantAdminStates.remove(telegramId);
                        yield generateSendMessage(telegramId, "Пользователь с таким ID не найден. Попробуйте снова.");
                    }
                } catch (NumberFormatException e) {
                    grantAdminStates.remove(telegramId);
                    yield generateSendMessage(telegramId,
                        "Ошибка: введённое значение не является корректным ID пользователя.");
                }
            }
            case CONFIRM -> {
                if ("Да".equalsIgnoreCase(message)) {
                    User targetUser = state.getTargetUser();
                    if (targetUser != null) {
                        targetUser.setUserRole(Role.ADMIN);
                        userService.saveUser(targetUser);
                        grantAdminStates.remove(telegramId);
                        yield generateSendMessage(telegramId,
                            String.format("Пользователю '%s' даны права администратора.", targetUser.getFullName()));
                    } else {
                        grantAdminStates.remove(telegramId);
                        yield generateSendMessage(telegramId, "Ошибка: пользователь для назначения роли не найден.");
                    }
                } else if ("Нет".equalsIgnoreCase(message)) {
                    grantAdminStates.remove(telegramId);
                    yield generateSendMessage(telegramId, "Назначение роли отменено.");
                } else {
                    grantAdminStates.remove(telegramId);
                    yield generateSendMessage(telegramId, "Неверная команда. Назначение роли отменено.");
                }
            }
        };
    }

    @Override
    public InteractionState getState() {
        return GRANT_ADMIN;
    }

    private void cleanRegistrationStates(long telegramId) {
        grantAdminStates.remove(telegramId);
        interactionStates.remove(telegramId);
    }
}
