package net.dunice.mk.rsmtelegrambot.handler.messagehandler;

import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.RSM_MEMBER;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.RSM_PARTNER;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.TRY_AGAIN;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.SELECTION_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.SELECTION_USER_TYPE_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.TRY_AGAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.PARTNER_REGISTRATION;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.USER_REGISTRATION;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.SELECT_REGISTRATION_TYPE;
import static net.dunice.mk.rsmtelegrambot.handler.state.step.SelectRegistrationStep.CONFIRM;
import static net.dunice.mk.rsmtelegrambot.handler.state.step.SelectRegistrationStep.REQUEST_REGISTRATION;
import static net.dunice.mk.rsmtelegrambot.handler.state.step.SelectRegistrationStep.RETRY_REGISTRATION;
import static net.dunice.mk.rsmtelegrambot.handler.state.step.SelectRegistrationStep.SWITCH_REGISTRATION_TYPE;

import lombok.RequiredArgsConstructor;
import net.dunice.mk.rsmtelegrambot.constant.Menu;
import net.dunice.mk.rsmtelegrambot.handler.state.BasicState;
import net.dunice.mk.rsmtelegrambot.handler.state.step.SelectRegistrationStep;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;

import java.util.EnumMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SelectRegistrationHandler implements MessageHandler {

    private final EnumMap<Menu, ReplyKeyboard> menus;
    private final Map<Long, SelectRegistrationStep> selectRegistrationSteps;
    private final Map<Long, BasicState> basicStates;
    private final UserRegistrationHandler userRegistrationHandler;
    private final PartnerRegistrationHandler partnerRegistrationHandler;

    @Override
    public SendMessage handle(String message, Long telegramId) {
        SelectRegistrationStep step = selectRegistrationSteps.get(telegramId);
        if (step == null) {
            selectRegistrationSteps.put(telegramId, (step = REQUEST_REGISTRATION));
        }
        return switch (step) {
            case REQUEST_REGISTRATION -> {
                selectRegistrationSteps.put(telegramId, CONFIRM);
                yield generateSendMessage(telegramId,
                    "Добро пожаловать! Вы не зарегистрированы, желаете пройти регистрацию? Ответьте 'Да' или 'Нет'.",
                    menus.get(SELECTION_MENU));
            }
            case CONFIRM -> {
                if ("Да".equalsIgnoreCase(message)) {
                    selectRegistrationSteps.put(telegramId, SWITCH_REGISTRATION_TYPE);
                    yield generateSendMessage(telegramId, "Кто вы?", menus.get(SELECTION_USER_TYPE_MENU));
                } else if ("Нет".equalsIgnoreCase(message)) {
                    selectRegistrationSteps.put(telegramId, RETRY_REGISTRATION);
                    yield generateSendMessage(telegramId, "Регистрация отменена.", menus.get(TRY_AGAIN_MENU));
                } else {
                    selectRegistrationSteps.put(telegramId, RETRY_REGISTRATION);
                    yield generateSendMessage(telegramId, "Неверная команда, регистрация отменена",
                        menus.get(TRY_AGAIN_MENU));
                }
            }
            case SWITCH_REGISTRATION_TYPE -> {
                if (RSM_MEMBER.equals(message)) {
                    selectRegistrationSteps.remove(telegramId);
                    basicStates.put(telegramId, USER_REGISTRATION);
                    yield userRegistrationHandler.handle(message, telegramId);
                }
                else if (RSM_PARTNER.equals(message)) {
                    selectRegistrationSteps.remove(telegramId);
                    basicStates.put(telegramId, PARTNER_REGISTRATION);
                    yield partnerRegistrationHandler.handle(message, telegramId);
                }
                else {
                    selectRegistrationSteps.put(telegramId, RETRY_REGISTRATION);
                    yield generateSendMessage(telegramId, "Неверная команда, регистрация отменена",
                        menus.get(TRY_AGAIN_MENU));
                }
            }
            case RETRY_REGISTRATION -> {
                if (TRY_AGAIN.equalsIgnoreCase(message)) {
                    selectRegistrationSteps.put(telegramId, REQUEST_REGISTRATION);
                    yield handle(message, telegramId);
                } else {
                    yield generateSendMessage(telegramId, "Неверная команда");
                }
            }
        };
    }

    @Override
    public BasicState getState() {
        return SELECT_REGISTRATION_TYPE;
    }
}
