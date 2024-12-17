package net.dunice.mk.rsmtelegrambot.handler;

import static net.dunice.mk.rsmtelegrambot.entity.Role.SUPER_USER;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.IN_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.SELECT_REGISTRATION_TYPE;

import lombok.RequiredArgsConstructor;
import net.dunice.mk.rsmtelegrambot.constant.Command;
import net.dunice.mk.rsmtelegrambot.constant.Menu;
import net.dunice.mk.rsmtelegrambot.entity.Partner;
import net.dunice.mk.rsmtelegrambot.entity.User;
import net.dunice.mk.rsmtelegrambot.handler.messagehandler.SelectRegistrationHandler;
import net.dunice.mk.rsmtelegrambot.handler.state.BasicState;
import net.dunice.mk.rsmtelegrambot.repository.PartnerRepository;
import net.dunice.mk.rsmtelegrambot.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CommandHandler implements BaseHandler {

    private final UserRepository userRepository;
    private final PartnerRepository partnerRepository;
    private final EnumMap<Menu, ReplyKeyboard> menus;
    private final SelectRegistrationHandler selectRegistrationHandler;
    private final MenuGenerator menuGenerator;
    private final Map<Long, BasicState> basicStates;

    @Override
    public SendMessage handle(String message, Long telegramId) {
        Optional<User> user = userRepository.findById(telegramId);
        if (user.isPresent()) {
            return switch (Command.getCommandByString(message)) {
                case START -> {
                    basicStates.put(telegramId, IN_MAIN_MENU);
                    yield menuGenerator.generateRoleSpecificMainMenu(telegramId, user.get().getUserRole());
                }
                case SUSTART -> {
                    user.get().setUserRole(SUPER_USER);
                    userRepository.save(user.get());
                    yield generateSendMessage(user.get().getTelegramId(), "Вы авторизованы как супер пользователь");
                }
            };
        } else {
            Optional<Partner> partner = partnerRepository.findById(telegramId);
            if (partner.isPresent()) {
                return generateSendMessage(telegramId, "Функционал меню партнера не реализован");
            } else {
                basicStates.put(telegramId, SELECT_REGISTRATION_TYPE);
                return selectRegistrationHandler.handle(null, telegramId);
            }
        }
    }
}
