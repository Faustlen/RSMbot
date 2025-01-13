package net.dunice.mk.rsmtelegrambot.handler.messagehandler;

import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.RSM_MEMBER;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.RSM_PARTNER;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.TRY_AGAIN;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.SELECTION_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.SELECTION_USER_TYPE_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.TRY_AGAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.BasicStep.PARTNER_REGISTRATION;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.BasicStep.SELECT_REGISTRATION_TYPE;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.BasicStep.USER_REGISTRATION;
import static net.dunice.mk.rsmtelegrambot.handler.state.SelectRegistrationState.SelectRegistrationStep.CHECK_CONFIRMATION;
import static net.dunice.mk.rsmtelegrambot.handler.state.SelectRegistrationState.SelectRegistrationStep.REQUEST_REGISTRATION;
import static net.dunice.mk.rsmtelegrambot.handler.state.SelectRegistrationState.SelectRegistrationStep.RETRY_REGISTRATION;
import static net.dunice.mk.rsmtelegrambot.handler.state.SelectRegistrationState.SelectRegistrationStep.SWITCH_REGISTRATION_TYPE;

import lombok.RequiredArgsConstructor;
import net.dunice.mk.rsmtelegrambot.constant.Menu;
import net.dunice.mk.rsmtelegrambot.dto.MessageDto;
import net.dunice.mk.rsmtelegrambot.handler.state.BasicState;
import net.dunice.mk.rsmtelegrambot.handler.state.BasicState.BasicStep;
import net.dunice.mk.rsmtelegrambot.handler.state.SelectRegistrationState;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;

import java.util.EnumMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SelectRegistrationHandler implements MessageHandler {

    private final EnumMap<Menu, ReplyKeyboard> menus;
    private final Map<Long, SelectRegistrationState> selectRegistrationStates;
    private final Map<Long, BasicState> basicStates;
    private final UserRegistrationHandler userRegistrationHandler;
    private final PartnerRegistrationHandler partnerRegistrationHandler;

    @Override
    public SendMessage handle(MessageDto messageDto, Long telegramId) {
        String text = messageDto.getText();

        SelectRegistrationState state = selectRegistrationStates.get(telegramId);
        if (state == null) {
            selectRegistrationStates.put(telegramId, (state = new SelectRegistrationState()));
        }
        return switch (state.getStep()) {
            case REQUEST_REGISTRATION -> {
                state.setStep(CHECK_CONFIRMATION);
                yield generateSendMessage(telegramId,
                    "Добро пожаловать! Вы не зарегистрированы, желаете пройти регистрацию? Ответьте 'Да' или 'Нет':",
                    menus.get(SELECTION_MENU));
            }
            case CHECK_CONFIRMATION -> {
                if ("Да".equalsIgnoreCase(text)) {
                    state.setStep(SWITCH_REGISTRATION_TYPE);
                    yield generateSendMessage(telegramId, "Кто вы?", menus.get(SELECTION_USER_TYPE_MENU));
                } else if ("Нет".equalsIgnoreCase(text)) {
                    state.setStep(RETRY_REGISTRATION);
                    yield generateSendMessage(telegramId, "Регистрация отменена.", menus.get(TRY_AGAIN_MENU));
                } else {
                    state.setStep(RETRY_REGISTRATION);
                    yield generateSendMessage(telegramId, "Неверная команда, регистрация отменена",
                        menus.get(TRY_AGAIN_MENU));
                }
            }
            case SWITCH_REGISTRATION_TYPE -> {
                if (RSM_MEMBER.equals(text)) {
                    selectRegistrationStates.remove(telegramId);
                    basicStates.get(telegramId).setStep(USER_REGISTRATION);
                    yield userRegistrationHandler.handle(messageDto, telegramId);
                }
                else if (RSM_PARTNER.equals(text)) {
                    selectRegistrationStates.remove(telegramId);
                    basicStates.get(telegramId).setStep(PARTNER_REGISTRATION);
                    yield partnerRegistrationHandler.handle(messageDto, telegramId);
                }
                else {
                    state.setStep(RETRY_REGISTRATION);
                    yield generateSendMessage(telegramId, "Неверная команда, регистрация отменена",
                        menus.get(TRY_AGAIN_MENU));
                }
            }
            case RETRY_REGISTRATION -> {
                if (TRY_AGAIN.equalsIgnoreCase(text)) {
                    state.setStep(REQUEST_REGISTRATION);
                    yield handle(messageDto, telegramId);
                } else {
                    yield generateSendMessage(telegramId, "Неверная команда");
                }
            }
        };
    }

    @Override
    public BasicStep getStep() {
        return SELECT_REGISTRATION_TYPE;
    }
}
