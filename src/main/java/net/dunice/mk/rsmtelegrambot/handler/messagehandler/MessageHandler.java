package net.dunice.mk.rsmtelegrambot.handler.messagehandler;

import net.dunice.mk.rsmtelegrambot.constant.InteractionState;
import net.dunice.mk.rsmtelegrambot.handler.BaseHandler;

public interface MessageHandler extends BaseHandler {
    InteractionState getState();
}
