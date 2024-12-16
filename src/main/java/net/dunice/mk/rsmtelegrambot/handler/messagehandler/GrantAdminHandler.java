package net.dunice.mk.rsmtelegrambot.handler.messagehandler;

import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.TO_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.TRY_AGAIN;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.GO_TO_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.SELECTION_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.TRY_AGAIN_OR_GO_TO_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.GRANT_ADMIN;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.IN_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.handler.state.step.GrantAdminStep.CONFIRM_ADMIN_CANDIDATE;
import static net.dunice.mk.rsmtelegrambot.handler.state.step.GrantAdminStep.FINISH;
import static net.dunice.mk.rsmtelegrambot.handler.state.step.GrantAdminStep.REQUEST_USER_ID;
import static net.dunice.mk.rsmtelegrambot.handler.state.step.GrantAdminStep.VERIFY_ADMIN_CANDIDATE;

import lombok.RequiredArgsConstructor;
import net.dunice.mk.rsmtelegrambot.constant.Menu;
import net.dunice.mk.rsmtelegrambot.entity.Role;
import net.dunice.mk.rsmtelegrambot.entity.User;
import net.dunice.mk.rsmtelegrambot.handler.MenuGenerator;
import net.dunice.mk.rsmtelegrambot.handler.state.BasicState;
import net.dunice.mk.rsmtelegrambot.handler.state.stateobject.GrantAdminState;
import net.dunice.mk.rsmtelegrambot.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GrantAdminHandler implements MessageHandler {

    private final UserRepository userRepository;
    private final Map<Long, GrantAdminState> grantAdminStates;
    private final Map<Long, BasicState> basicStates;
    private final EnumMap<Menu, ReplyKeyboard> menus;
    private final MenuGenerator menuGenerator;

    @Override
    public SendMessage handle(String message, Long telegramId) {
        GrantAdminState state = grantAdminStates.get(telegramId);
        if (state == null) {
            grantAdminStates.put(telegramId, (state = new GrantAdminState()));
        }
        return switch (state.getStep()) {
            case REQUEST_USER_ID -> {
                state.setStep(VERIFY_ADMIN_CANDIDATE);
                yield generateSendMessage(telegramId,
                    "Введите ID пользователя, которому хотите дать права администратора");
            }
            case VERIFY_ADMIN_CANDIDATE -> {
                try {
                    Long targetUserId = Long.parseLong(message);
                    Optional<User> targetUser = userRepository.findById(targetUserId);
                    if (targetUser.isPresent()) {
                        state.setTargetUser(targetUser.get());
                        state.setStep(CONFIRM_ADMIN_CANDIDATE);
                        yield generateSendMessage(telegramId,
                            String.format("Хотите дать права администратора пользователю '%s'?",
                                targetUser.get().getFullName()),
                            menus.get(SELECTION_MENU));
                    } else {
                        state.setStep(FINISH);
                        yield generateSendMessage(telegramId, "Пользователь с таким ID не найден. Попробуйте снова.",
                            menus.get(TRY_AGAIN_OR_GO_TO_MAIN_MENU));
                    }
                } catch (NumberFormatException e) {
                    state.setStep(FINISH);
                    yield generateSendMessage(telegramId,
                        "Ошибка: введённое значение не является корректным ID пользователя.",
                        menus.get(TRY_AGAIN_OR_GO_TO_MAIN_MENU));
                }
            }
            case CONFIRM_ADMIN_CANDIDATE -> {
                if ("Да".equalsIgnoreCase(message)) {
                    User targetUser = state.getTargetUser();
                    targetUser.setUserRole(Role.ADMIN);
                    userRepository.save(targetUser);
                    state.setStep(FINISH);
                    yield generateSendMessage(telegramId,
                        String.format("Пользователю '%s' даны права администратора.", targetUser.getFullName()),
                        menus.get(GO_TO_MAIN_MENU));
                } else if ("Нет".equalsIgnoreCase(message)) {
                    state.setStep(FINISH);
                    yield generateSendMessage(telegramId, "Назначение роли отменено.",
                        menus.get(TRY_AGAIN_OR_GO_TO_MAIN_MENU));
                } else {
                    state.setStep(FINISH);
                    yield generateSendMessage(telegramId, "Неверная команда. Назначение роли отменено.",
                        menus.get(TRY_AGAIN_OR_GO_TO_MAIN_MENU));
                }
            }
            case FINISH -> {
                if (TO_MAIN_MENU.equalsIgnoreCase(message)) {
                    grantAdminStates.remove(telegramId);
                    basicStates.put(telegramId, IN_MAIN_MENU);
                    yield menuGenerator.generateRoleSpecificMainMenu(telegramId,
                        userRepository.findByTelegramId(telegramId).get().getUserRole());
                } else if (TRY_AGAIN.equalsIgnoreCase(message)) {
                    grantAdminStates.get(telegramId).setStep(REQUEST_USER_ID);
                    yield handle(message, telegramId);
                } else {
                    yield generateSendMessage(telegramId, "Неверная команда");
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
        basicStates.remove(telegramId);
    }
}
