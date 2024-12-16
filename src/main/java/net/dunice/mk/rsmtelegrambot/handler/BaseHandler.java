package net.dunice.mk.rsmtelegrambot.handler;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

public interface BaseHandler extends MessageGenerator {

    public SendMessage handle(String message, Long telegramId);
}
