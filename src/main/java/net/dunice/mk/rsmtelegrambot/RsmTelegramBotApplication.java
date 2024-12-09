package net.dunice.mk.rsmtelegrambot;

import net.dunice.mk.rsmtelegrambot.entity.User;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RsmTelegramBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(RsmTelegramBotApplication.class, args);
        User user = new User();
    }

}
