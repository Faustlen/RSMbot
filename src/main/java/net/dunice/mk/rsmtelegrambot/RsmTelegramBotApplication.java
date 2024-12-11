package net.dunice.mk.rsmtelegrambot;

import net.dunice.mk.rsmtelegrambot.service.TelegramBot;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@SpringBootApplication
public class RsmTelegramBotApplication {

    public static void main(String[] args) {
        var context = SpringApplication.run(RsmTelegramBotApplication.class, args);

        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            TelegramBot telegramBot = context.getBean(TelegramBot.class);
            botsApi.registerBot(telegramBot);
            telegramBot.setBotCommands();
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
