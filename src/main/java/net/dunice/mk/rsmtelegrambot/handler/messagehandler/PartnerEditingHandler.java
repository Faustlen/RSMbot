package net.dunice.mk.rsmtelegrambot.handler.messagehandler;

import lombok.RequiredArgsConstructor;
import net.dunice.mk.rsmtelegrambot.dto.MessageDto;
import net.dunice.mk.rsmtelegrambot.handler.state.BasicState;
import net.dunice.mk.rsmtelegrambot.handler.state.PartnerEditingState;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.Map;

import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.CANCEL;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.TO_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.BasicStep.PARTNER_EDITING;

@Service
@RequiredArgsConstructor
public class PartnerEditingHandler implements MessageHandler{
    private final Map<Long, PartnerEditingState> partnerEditingStates;

    @Override
    public BasicState.BasicStep getStep() {
        return PARTNER_EDITING;
    }

    @Override
    public PartialBotApiMethod<Message> handle(MessageDto messageDto, Long telegramId) {
        String text = messageDto.getText();
        PartnerEditingState state = partnerEditingStates.get(telegramId);
        if (state == null) {
            partnerEditingStates.put(telegramId, (state = new PartnerEditingState()));
        }

        if (StringUtils.equalsAny(text, CANCEL, TO_MAIN_MENU)) {
            return goToMainMenu(telegramId);
        }
    }
}
