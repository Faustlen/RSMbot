package net.dunice.mk.rsmtelegrambot.service.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dunice.mk.rsmtelegrambot.entity.User;
import net.dunice.mk.rsmtelegrambot.event.BroadcastEvent;
import net.dunice.mk.rsmtelegrambot.service.TelegramBot;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BroadcastEventListener {

    private final TelegramBot bot;

    @EventListener
    public void handleBroadcastEvent(BroadcastEvent event) {
        List<User> users = event.getUsers();
        for (User user : users) {
            SendMessage message = bot.generateSendMessage(user.getTelegramId(), event.getMessage());
            bot.sendMessage(message);
        }
    }
}
