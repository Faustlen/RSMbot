package net.dunice.mk.rsmtelegrambot.handler;

import static net.dunice.mk.rsmtelegrambot.entity.Role.SUPER_USER;

import lombok.RequiredArgsConstructor;
import net.dunice.mk.rsmtelegrambot.constant.Command;
import net.dunice.mk.rsmtelegrambot.constant.Menu;
import net.dunice.mk.rsmtelegrambot.entity.User;
import net.dunice.mk.rsmtelegrambot.handler.messagehandler.RegistrationHandler;
import net.dunice.mk.rsmtelegrambot.handler.messagehandler.UserMenuHandler;
import net.dunice.mk.rsmtelegrambot.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;

import java.util.EnumMap;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CommandHandler implements BaseHandler {

    private final UserRepository userRepository;
    private final EnumMap<Menu, ReplyKeyboardMarkup> menus;
    private final UserMenuHandler userMenuHandler;
    private final RegistrationHandler registrationHandler;

    @Override
    public SendMessage handle(String message, Long telegramId) {
        Optional<User> user = userRepository.findById(telegramId);
        if (user.isPresent()) {
            return switch (Command.getCommandByString(message)) {
                case START -> userMenuHandler.handle(message, telegramId);
                case SUSTART -> {
                    user.get().setUserRole(SUPER_USER);
                    userRepository.save(user.get());
                    yield generateSendMessage(user.get().getTelegramId(), "Вы авторизованы как супер пользователь");
                }
            };
        } else {
            return registrationHandler.handle(null, telegramId);
        }
    }
}
