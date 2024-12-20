package net.dunice.mk.rsmtelegrambot.handler;

import net.dunice.mk.rsmtelegrambot.dto.MessageDto;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Message;

public interface BaseHandler extends MessageGenerator {
    PartialBotApiMethod<Message> handle(MessageDto messageDto, Long telegramId);
}
