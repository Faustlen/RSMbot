package net.dunice.mk.rsmtelegrambot.handler.clickhandler;

import net.dunice.mk.rsmtelegrambot.constants.InteractionState;

public interface ClickHandler {

    String handleClick(String data, Long telegramId);

    InteractionState getState();
}
