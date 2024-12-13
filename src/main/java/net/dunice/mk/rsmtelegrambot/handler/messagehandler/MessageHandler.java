package net.dunice.mk.rsmtelegrambot.handler.messagehandler;

import net.dunice.mk.rsmtelegrambot.handler.state.BasicState;
import net.dunice.mk.rsmtelegrambot.handler.BaseHandler;

public interface MessageHandler extends BaseHandler {
    BasicState getState();
}
