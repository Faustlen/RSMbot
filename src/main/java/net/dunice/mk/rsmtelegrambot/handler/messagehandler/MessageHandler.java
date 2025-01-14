package net.dunice.mk.rsmtelegrambot.handler.messagehandler;

import net.dunice.mk.rsmtelegrambot.handler.BaseHandler;
import net.dunice.mk.rsmtelegrambot.handler.state.BasicState.BasicStep;

public interface MessageHandler extends BaseHandler {
    BasicStep getStep();
}
