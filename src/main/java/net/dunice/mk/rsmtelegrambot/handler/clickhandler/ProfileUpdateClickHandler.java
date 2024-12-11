package net.dunice.mk.rsmtelegrambot.handler.clickhandler;

import lombok.RequiredArgsConstructor;
import net.dunice.mk.rsmtelegrambot.constants.InteractionState;
import net.dunice.mk.rsmtelegrambot.handler.messagehandler.ProfileUpdateMessageHandler;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProfileUpdateClickHandler implements ClickHandler {

    private final ProfileUpdateMessageHandler messageHandler;

    @Override
    public String handleClick(String data, Long telegramId) {
        return messageHandler.handleMessage(data, telegramId);
    }

    @Override
    public InteractionState getState() {
        return InteractionState.USER_MAIN_MENU;
    }
}
