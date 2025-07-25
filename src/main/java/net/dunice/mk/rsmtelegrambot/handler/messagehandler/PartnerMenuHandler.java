package net.dunice.mk.rsmtelegrambot.handler.messagehandler;

import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.ADD_STOCK;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.NEW_CHECK;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.PARTNERS_LIST;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.PERIOD_ANALYTICS;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.STOCKS_LIST;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.UPDATE_PROFILE;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.VERIFICATION_CODE;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.BasicStep;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.BasicStep.CREATE_CHECK;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.BasicStep.CREATE_STOCK;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.BasicStep.IN_PARTNER_MENU;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.BasicStep.PARTNER_EDITING;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.BasicStep.REQUEST_VERIFICATION_CODE;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.BasicStep.SHOW_ANALYTICS;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.BasicStep.SHOW_PARTNERS;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.BasicStep.SHOW_STOCKS;

import lombok.RequiredArgsConstructor;
import net.dunice.mk.rsmtelegrambot.dto.MessageDto;
import net.dunice.mk.rsmtelegrambot.handler.state.BasicState;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PartnerMenuHandler implements MessageHandler {

    private final Map<Long, BasicState> basicStates;
    private final ShowAnalyticsHandler showAnalyticsHandler;
    private final RequestVerificationCodeHandler requestCodeHandler;
    private final CreateCheckHandler createCheckHandler;
    private final PartnerEditingHandler partnerEditingHandler;
    private final CreateStockHandler createStockHandler;
    private final ShowPartnersHandler showPartnersHandler;
    private final ShowStocksHandler showStocksHandler;

    @Override
    public BasicStep getStep() {
        return IN_PARTNER_MENU;
    }

    @Override
    public PartialBotApiMethod<Message> handle(MessageDto messageDto, Long telegramId) {
        String text = messageDto.getText();
        if (text == null) {
            return generateSendMessage(telegramId, "Неверная команда");
        }
        Optional<PartialBotApiMethod<Message>> sendMessage = Optional.ofNullable(
            switch (text) {
                case PARTNERS_LIST -> {
                    basicStates.get(telegramId).setStep(SHOW_PARTNERS);
                    yield showPartnersHandler.handle(messageDto, telegramId);
                }
                case PERIOD_ANALYTICS -> {
                    basicStates.get(telegramId).setStep(SHOW_ANALYTICS);
                    yield showAnalyticsHandler.handle(messageDto, telegramId);
                }
                case VERIFICATION_CODE -> {
                    basicStates.get(telegramId).setStep(REQUEST_VERIFICATION_CODE);
                    yield requestCodeHandler.handle(messageDto, telegramId);
                }
                case NEW_CHECK -> {
                    basicStates.get(telegramId).setStep(CREATE_CHECK);
                    yield createCheckHandler.handle(messageDto, telegramId);
                }
                case UPDATE_PROFILE -> {
                    basicStates.get(telegramId).setStep(PARTNER_EDITING);
                    yield partnerEditingHandler.handle(messageDto, telegramId);
                }
                case ADD_STOCK -> {
                    basicStates.get(telegramId).setStep((CREATE_STOCK));
                    messageDto.setText(telegramId.toString());
                    yield createStockHandler.handle(messageDto, telegramId);
                }
                case STOCKS_LIST -> {
                    basicStates.get(telegramId).setStep((SHOW_STOCKS));
                    yield showStocksHandler.handle(messageDto, telegramId);
                }
                default -> null;
            }
        );
        return sendMessage.orElseGet(() ->
            generateSendMessage(telegramId, "Неверная команда - " + text));
    }
}
