package net.dunice.mk.rsmtelegrambot.handler;

import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Message;

public interface BaseHandler extends MessageGenerator {

    public PartialBotApiMethod<Message> handle(String message, Long telegramId);
}
