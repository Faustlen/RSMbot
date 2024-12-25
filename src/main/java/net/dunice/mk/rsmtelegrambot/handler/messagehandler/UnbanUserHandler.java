package net.dunice.mk.rsmtelegrambot.handler.messagehandler;

import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.TO_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.GO_TO_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.SELECTION_MENU;
import static net.dunice.mk.rsmtelegrambot.entity.Role.USER;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.IN_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.UNBAN_USER;
import static net.dunice.mk.rsmtelegrambot.handler.state.stateobject.BanUserState.BanUserStep.CONFIRM_USER_TO_BAN;
import static net.dunice.mk.rsmtelegrambot.handler.state.stateobject.BanUserState.BanUserStep.VERIFY_USER_TO_BAN;

import lombok.RequiredArgsConstructor;
import net.dunice.mk.rsmtelegrambot.constant.Menu;
import net.dunice.mk.rsmtelegrambot.dto.MessageDto;
import net.dunice.mk.rsmtelegrambot.entity.User;
import net.dunice.mk.rsmtelegrambot.handler.MenuGenerator;
import net.dunice.mk.rsmtelegrambot.handler.state.BasicState;
import net.dunice.mk.rsmtelegrambot.handler.state.stateobject.BanUserState;
import net.dunice.mk.rsmtelegrambot.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UnbanUserHandler implements MessageHandler {

    private final UserRepository userRepository;
    private final Map<Long, BasicState> basicStates;
    private final EnumMap<Menu, ReplyKeyboard> menus;
    private final MenuGenerator menuGenerator;
    private final Map<Long, BanUserState> banUserStates;

    @Override
    public BasicState getState() {
        return UNBAN_USER;
    }

    @Override
    public PartialBotApiMethod<Message> handle(MessageDto messageDto, Long telegramId) {
        String text = messageDto.getText();
        BanUserState state = banUserStates.get(telegramId);
        if (state == null) {
            banUserStates.put(telegramId, (state = new BanUserState()));
        }

        if (TO_MAIN_MENU.equals(text)) {
            return goToMainMenu(telegramId);
        }
        return switch (state.getStep()) {
            case REQUEST_USER_ID -> {
                state.setStep(VERIFY_USER_TO_BAN);
                yield generateSendMessage(telegramId,
                    "Введите ID пользователя, которого хотите разбанить");
            }
            case VERIFY_USER_TO_BAN -> {
                try {
                    Long targetUserId = Long.parseLong(text);
                    Optional<User> targetUser = userRepository.findById(targetUserId);
                    if (targetUser.isPresent()) {
                        if (targetUserId.equals(telegramId)) {
                            yield generateSendMessage(telegramId, "Вы ввели собственный ID. Повторите ввод:");
                        } else if (!targetUser.get().isBanned()) {
                            yield generateSendMessage(telegramId,
                                "Пользователь '%s' не находится в бане. Повторите ввод:".formatted(
                                    targetUser.get().getFullName()));
                        }
                        state.setTargetUser(targetUser.get());
                        state.setStep(CONFIRM_USER_TO_BAN);
                        yield generateSendMessage(telegramId,
                            String.format("Хотите разбанить пользователя '%s'?", targetUser.get().getFullName()),
                            menus.get(SELECTION_MENU));
                    } else {
                        yield generateSendMessage(telegramId, "Пользователь с таким ID не найден. Повторите ввод:");
                    }
                } catch (NumberFormatException e) {
                    yield generateSendMessage(telegramId,
                        "Ошибка: введённое значение не является корректным ID пользователя. Повторите ввод:");
                }
            }
            case CONFIRM_USER_TO_BAN -> {
                if ("Да".equalsIgnoreCase(text)) {
                    User targetUser = state.getTargetUser();
                    targetUser.setBanned(false);
                    targetUser.setUserRole(USER);
                    userRepository.save(targetUser);
                    yield generateSendMessage(telegramId,
                        String.format("Пользователь '%s' разбанен.", targetUser.getFullName()),
                        menus.get(GO_TO_MAIN_MENU));
                } else if ("Нет".equalsIgnoreCase(text)) {
                    yield generateSendMessage(telegramId, "Разбан пользователя отменен.",
                        menus.get(GO_TO_MAIN_MENU));
                } else {
                    yield generateSendMessage(telegramId, "Неверная команда. Разбан пользователя отменен.",
                        menus.get(GO_TO_MAIN_MENU));
                }
            }
        };
    }

    private SendMessage goToMainMenu(Long telegramId) {
        banUserStates.remove(telegramId);
        basicStates.put(telegramId, IN_MAIN_MENU);
        return menuGenerator.generateRoleSpecificMainMenu(telegramId,
            userRepository.findByTelegramId(telegramId).get().getUserRole());
    }
}

