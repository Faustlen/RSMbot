package net.dunice.mk.rsmtelegrambot.handler.messagehandler;

import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.TO_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.GO_TO_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.SELECTION_MENU;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.GRANT_ADMIN;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.IN_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.handler.state.stateobject.GrantAdminState.GrantAdminStep.CONFIRM_ADMIN_CANDIDATE;
import static net.dunice.mk.rsmtelegrambot.handler.state.stateobject.GrantAdminState.GrantAdminStep.VERIFY_ADMIN_CANDIDATE;

import lombok.RequiredArgsConstructor;
import net.dunice.mk.rsmtelegrambot.constant.Menu;
import net.dunice.mk.rsmtelegrambot.dto.MessageDto;
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
    private final Map<Long, BasicState> basicStates;
    private final EnumMap<Menu, ReplyKeyboard> menus;
    private final MenuGenerator menuGenerator;
    private final Map<Long, GrantAdminState> grantAdminStates;

    @Override
    public SendMessage handle(MessageDto messageDto, Long telegramId) {
        String text = messageDto.getText();
        GrantAdminState state = grantAdminStates.get(telegramId);
        if (state == null) {
            grantAdminStates.put(telegramId, (state = new GrantAdminState()));
        }
        ReplyKeyboard mainMenu = menus.get(GO_TO_MAIN_MENU);

        if (TO_MAIN_MENU.equals(text)) {
            return goToMainMenu(telegramId);
        }

        return switch (state.getStep()) {
            case REQUEST_USER_ID -> {
                state.setStep(VERIFY_ADMIN_CANDIDATE);
                yield generateSendMessage(telegramId,
                    "Введите ID пользователя, которому хотите дать права администратора");
            }
            case VERIFY_ADMIN_CANDIDATE -> {
                try {
                    Long targetUserId = Long.parseLong(text);
                    Optional<User> targetUser = userRepository.findById(targetUserId);
                    if (targetUser.isPresent()) {
                        state.setTargetUser(targetUser.get());
                        state.setStep(CONFIRM_ADMIN_CANDIDATE);
                        yield generateSendMessage(telegramId,
                            String.format("Хотите дать права администратора пользователю '%s'?",
                                targetUser.get().getFullName()),
                            menus.get(SELECTION_MENU));
                    } else {
                        yield generateSendMessage(telegramId, "Пользователь с таким ID не найден. Повторите ввод:");
                    }
                } catch (NumberFormatException e) {
                    yield generateSendMessage(telegramId,
                        "Ошибка: введённое значение не является корректным ID пользователя. Повторите ввод:");
                }
            }
            case CONFIRM_ADMIN_CANDIDATE -> {
                if ("Да".equalsIgnoreCase(text)) {
                    User targetUser = state.getTargetUser();
                    targetUser.setUserRole(Role.ADMIN);
                    userRepository.save(targetUser);
                    yield generateSendMessage(telegramId,
                        String.format("Пользователю '%s' даны права администратора.", targetUser.getFullName()),
                        mainMenu);
                } else if ("Нет".equalsIgnoreCase(text)) {
                    yield generateSendMessage(telegramId, "Назначение роли отменено.",
                        mainMenu);
                } else {
                    yield generateSendMessage(telegramId, "Неверная команда. Назначение роли отменено.",
                        mainMenu);
                }
            }
        };
    }

    @Override
    public BasicState getState() {
        return GRANT_ADMIN;
    }

    private SendMessage goToMainMenu(Long telegramId) {
        grantAdminStates.remove(telegramId);
        basicStates.put(telegramId, IN_MAIN_MENU);
        return menuGenerator.generateRoleSpecificMainMenu(telegramId,
            userRepository.findByTelegramId(telegramId).get().getUserRole());
    }
}
