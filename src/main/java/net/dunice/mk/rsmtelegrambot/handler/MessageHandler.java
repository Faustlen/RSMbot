package net.dunice.mk.rsmtelegrambot.handler;

import net.dunice.mk.rsmtelegrambot.constant.InteractionState;

public interface MessageHandler extends BaseHandler {
    InteractionState getState();
}
