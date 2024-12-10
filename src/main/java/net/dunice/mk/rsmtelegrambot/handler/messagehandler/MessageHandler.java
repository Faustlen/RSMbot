package net.dunice.mk.rsmtelegrambot.handler.messagehandler;

import net.dunice.mk.rsmtelegrambot.constants.InteractionState;

public interface MessageHandler {

    String handleMessage(String message, Long telegramId);

    InteractionState getState();

}
