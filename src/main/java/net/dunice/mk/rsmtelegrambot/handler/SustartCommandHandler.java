package net.dunice.mk.rsmtelegrambot.handler;

import static net.dunice.mk.rsmtelegrambot.constant.InteractionState.REGISTRATION;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.SELECTION_MENU;
import static net.dunice.mk.rsmtelegrambot.entity.Role.SUPER_USER;

import lombok.RequiredArgsConstructor;
import net.dunice.mk.rsmtelegrambot.constant.InteractionState;
import net.dunice.mk.rsmtelegrambot.constant.Menu;
import net.dunice.mk.rsmtelegrambot.entity.User;
import net.dunice.mk.rsmtelegrambot.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SustartCommandHandler implements BaseHandler {

    private final UserRepository userRepository;
    private final Map<Long, InteractionState> interactionStates;
    private final EnumMap<Menu, ReplyKeyboardMarkup> menus;

    @Override
    public SendMessage handleMessage(String message, Long telegramId) {
        Optional<User> user = userRepository.findByTelegramId(telegramId);
        if (user.isPresent()) {
            user.get().setUserRole(SUPER_USER);
            userRepository.save(user.get());
            return generateSendMessage(telegramId, "Вы авторизованы как супер пользователь", null);
        } else {
            return requireRegistration(telegramId);
        }
    }

    private SendMessage requireRegistration(long telegramId) {
        interactionStates.put(telegramId, REGISTRATION);
        return generateSendMessage(telegramId,
            "Добро пожаловать! Вы не зарегистрированы, желаете пройти регистрацию? Ответьте 'Да' или 'Нет'.",
            menus.get(SELECTION_MENU));
    }
}
