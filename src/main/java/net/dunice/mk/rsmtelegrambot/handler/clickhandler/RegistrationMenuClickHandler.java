package net.dunice.mk.rsmtelegrambot.handler.clickhandler;

import lombok.RequiredArgsConstructor;
import net.dunice.mk.rsmtelegrambot.constants.InteractionState;
import net.dunice.mk.rsmtelegrambot.handler.messagehandler.RegistrationMessageHandler;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegistrationMenuClickHandler implements ClickHandler {

    private final RegistrationMessageHandler messageHandler;

    @Override
    public String handleClick(String data, Long telegramId) {
        return messageHandler.handleMessage(data, telegramId);
    }

    @Override
    public InteractionState getState() {
        return InteractionState.REGISTRATION;
    }
}
