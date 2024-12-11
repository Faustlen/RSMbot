package net.dunice.mk.rsmtelegrambot.config;

import net.dunice.mk.rsmtelegrambot.constants.InteractionState;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

@Configuration
public class AppConfig {

    @Bean
    public EnumMap<InteractionState, ReplyKeyboardMarkup> getReplyKeyboardMenus(
            ReplyKeyboardMarkup userMainMenu,
            ReplyKeyboardMarkup registrationMenu,
            ReplyKeyboardMarkup superUserMainMenu,
            ReplyKeyboardMarkup adminMainMenu) {
        EnumMap<InteractionState, ReplyKeyboardMarkup> menus = new EnumMap<>(InteractionState.class);
        menus.put(InteractionState.REGISTRATION, registrationMenu);
        menus.put(InteractionState.USER_MAIN_MENU, userMainMenu);
        menus.put(InteractionState.SUPER_USER_MAIN_MENU, superUserMainMenu);
        menus.put(InteractionState.ADMIN_MAIN_MENU, adminMainMenu);
        return menus;
    }

    @Bean("userMainMenu")
    public ReplyKeyboardMarkup getUserMainMenu() {
        return getBaseReplyMenu();
    }

    @Bean("adminMainMenu")
    public ReplyKeyboardMarkup getAdminMainMenu() {
        ReplyKeyboardMarkup replyMarkup = getBaseReplyMenu();
        List<KeyboardRow> keyboard = replyMarkup.getKeyboard();
        KeyboardRow row = new KeyboardRow();
        row.add("Добавить партнёра");
        row.add("Добавить мероприятие");
        keyboard.add(row);
        return replyMarkup;
    }

    @Bean("superUserMainMenu")
    public ReplyKeyboardMarkup getSuperUserMainMenu() {
        ReplyKeyboardMarkup replyMarkup = getBaseReplyMenu();
        List<KeyboardRow> keyboard = replyMarkup.getKeyboard();
        KeyboardRow row = new KeyboardRow();
        row.add("Назначить админа");
        row.add("Добавить партнёра");
        row.add("Добавить мероприятие");
        keyboard.add(row);
        return replyMarkup;
    }

    @Bean("registrationMenu")
    public ReplyKeyboardMarkup getRegistrationMenu() {
        ReplyKeyboardMarkup replyMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add("Да");
        row.add("Нет");
        keyboard.add(row);
        replyMarkup.setKeyboard(keyboard);
        replyMarkup.setResizeKeyboard(true);
        replyMarkup.setOneTimeKeyboard(true);
        return replyMarkup;
    }

    private ReplyKeyboardMarkup getBaseReplyMenu() {
        ReplyKeyboardMarkup replyMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add("Партнёры");
        row.add("Мероприятия");
        row.add("Изменить профиль");
        keyboard.add(row);
        replyMarkup.setKeyboard(keyboard);
        replyMarkup.setResizeKeyboard(true);
        replyMarkup.setOneTimeKeyboard(false);
        return replyMarkup;
    }
}