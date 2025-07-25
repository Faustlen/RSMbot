package net.dunice.mk.rsmtelegrambot.handler;

import static net.dunice.mk.rsmtelegrambot.entity.Role.SUPER_USER;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.BasicStep.IN_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.BasicStep.IN_PARTNER_MENU;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.BasicStep.SELECT_REGISTRATION_TYPE;

import lombok.RequiredArgsConstructor;
import net.dunice.mk.rsmtelegrambot.constant.Command;
import net.dunice.mk.rsmtelegrambot.dto.MessageDto;
import net.dunice.mk.rsmtelegrambot.entity.Partner;
import net.dunice.mk.rsmtelegrambot.entity.User;
import net.dunice.mk.rsmtelegrambot.handler.messagehandler.SelectRegistrationHandler;
import net.dunice.mk.rsmtelegrambot.handler.state.BasicState;
import net.dunice.mk.rsmtelegrambot.repository.PartnerRepository;
import net.dunice.mk.rsmtelegrambot.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CommandHandler implements BaseHandler {

    private final UserRepository userRepository;
    private final PartnerRepository partnerRepository;
    private final SelectRegistrationHandler selectRegistrationHandler;
    private final MenuGenerator menuGenerator;
    private final Map<Long, BasicState> basicStates;

    @Override
    public SendMessage handle(MessageDto messageDto, Long telegramId) {
        Optional<User> userOptional = userRepository.findById(telegramId);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if (user.isBanned()) {
                return generateSendMessage(telegramId, """
                    К сожалению, вы находитесь в списке пользователей, которым ограничен доступ к этому боту.
                    Если вы считаете, что это ошибка, обратитесь к своему руководителю РСМ.
                    """);
            } else {
                return switch (Command.getCommandByString(messageDto.getText())) {
                    case START -> {
                        basicStates.put(telegramId, new BasicState(user, IN_MAIN_MENU));
                        yield menuGenerator.generateRoleSpecificMainMenu(telegramId, user.getUserRole());
                    }
                    case SUSTART -> {
                        userOptional = userRepository.findFirstByUserRole(SUPER_USER);
                        if (userOptional.isPresent() &&
                            !userOptional.get().getTelegramId().equals(user.getTelegramId())) {
                            yield generateSendMessage(user.getTelegramId(),
                                "Назначение отклонено: супер пользователь уже существует.");
                        } else {
                            user.setUserRole(SUPER_USER);
                            userRepository.save(user);
                            yield generateSendMessage(user.getTelegramId(), "Вы авторизованы как супер пользователь");
                        }
                    }
                };
            }
        } else {
            Optional<Partner> partner = partnerRepository.findById(telegramId);
            BasicState basicState = new BasicState();
            if (partner.isPresent()) {
                basicState.setStep(IN_PARTNER_MENU);
                basicStates.put(telegramId, basicState);
                return generateSendMessage(telegramId, "Выберите раздел:", menuGenerator.getPartnerMenu(partner.get()));
            } else {
                basicState.setStep(SELECT_REGISTRATION_TYPE);
                basicStates.put(telegramId, basicState);
                return selectRegistrationHandler.handle(messageDto, telegramId);
            }
        }
    }
}
