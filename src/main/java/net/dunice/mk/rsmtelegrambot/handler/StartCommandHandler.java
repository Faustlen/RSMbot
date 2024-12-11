package net.dunice.mk.rsmtelegrambot.handler;

import lombok.RequiredArgsConstructor;
import net.dunice.mk.rsmtelegrambot.constant.InteractionState;
import net.dunice.mk.rsmtelegrambot.constant.Menu;
import net.dunice.mk.rsmtelegrambot.entity.Role;
import net.dunice.mk.rsmtelegrambot.entity.User;
import net.dunice.mk.rsmtelegrambot.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import static net.dunice.mk.rsmtelegrambot.constant.InteractionState.*;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.*;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.ADMIN_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.entity.Role.ADMIN;
import static net.dunice.mk.rsmtelegrambot.entity.Role.SUPER_USER;

@Service
@RequiredArgsConstructor
public class StartCommandHandler implements BaseHandler {

    private final UserRepository userRepository;
    private final Map<Long, InteractionState> interactionStates;
    private final EnumMap<Menu, ReplyKeyboardMarkup> menus;

    @Override
    public SendMessage handleMessage(String message, Long telegramId) {
        Optional<User> user = userRepository.findByTelegramId(telegramId);
        if (user.isPresent()) {
            Role role = user.get().getUserRole();
            Menu menu = null;
            InteractionState state = null;
            if (role == SUPER_USER) {
                menu = SUPERUSER_MAIN_MENU;
                state = IN_SUPERUSER_MAIN_MENU;
            }
            else if (role == ADMIN) {
                menu = ADMIN_MAIN_MENU;
                state = InteractionState.IN_ADMIN_MAIN_MENU;
            }
            else {
                menu = USER_MAIN_MENU;
                state = IN_USER_MAIN_MENU;
            }
            interactionStates.put(telegramId, state);
            return generateSendMessage(telegramId,"Добро пожаловать! Выберите действие:", menus.get(menu));
        }
        else return requireRegistration(telegramId);
    }

    private SendMessage requireRegistration(long telegramId) {
        interactionStates.put(telegramId, REGISTRATION);
        return generateSendMessage(telegramId,
                "Добро пожаловать! Вы не зарегистрированы, желаете пройти регистрацию? Ответьте 'Да' или 'Нет'.",
                menus.get(SELECTION_MENU));
    }
}
