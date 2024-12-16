package net.dunice.mk.rsmtelegrambot.handler.messagehandler;

import net.dunice.mk.rsmtelegrambot.handler.BaseHandler;
import net.dunice.mk.rsmtelegrambot.handler.state.BasicState;

public interface MessageHandler extends BaseHandler {
    BasicState getState();
}
