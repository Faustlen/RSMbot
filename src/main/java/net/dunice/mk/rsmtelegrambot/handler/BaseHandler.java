package net.dunice.mk.rsmtelegrambot.handler;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

public interface BaseHandler extends MessageSender {

    public SendMessage handle(String message, Long telegramId);
}
