package net.dunice.mk.rsmtelegrambot.service.util;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
public interface TelegramAdapter {

    void sendMessage(Long chatId, String text) throws TelegramApiException;
}
