package net.dunice.mk.rsmtelegrambot.handler;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;

public interface MessageGenerator {

    default SendMessage generateSendMessage(Long telegramId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(telegramId);
        message.setText(text);
        return message;
    }

    default SendMessage generateSendMessage(Long telegramId, String text, ReplyKeyboard keyboardMarkup) {
        SendMessage message = new SendMessage();
        message.setChatId(telegramId);
        message.setText(text);
        message.setReplyMarkup(keyboardMarkup);
        return message;
    }
}
