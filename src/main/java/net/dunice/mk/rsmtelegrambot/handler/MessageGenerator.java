package net.dunice.mk.rsmtelegrambot.handler;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

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

    default SendPhoto generateImageMessage(Long telegramId, String text, ReplyKeyboard keyboardMarkup, byte[] image) {
        try (InputStream inputStream = new ByteArrayInputStream(image)) {
            SendPhoto sendPhoto = new SendPhoto();
            sendPhoto.setChatId(telegramId);
            sendPhoto.setCaption(text);
            sendPhoto.setReplyMarkup(keyboardMarkup);
            InputFile inputFile = new InputFile(inputStream, "image");
            sendPhoto.setPhoto(inputFile);
            return sendPhoto;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
