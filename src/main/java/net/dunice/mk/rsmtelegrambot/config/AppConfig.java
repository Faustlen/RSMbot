package net.dunice.mk.rsmtelegrambot.config;

import net.dunice.mk.rsmtelegrambot.constant.InteractionState;
import net.dunice.mk.rsmtelegrambot.constant.Menu;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class AppConfig {

    @Bean
    public EnumMap<Menu, ReplyKeyboardMarkup> getReplyKeyboardMenus(
            ReplyKeyboardMarkup userMainMenu,
            ReplyKeyboardMarkup selectionMenu,
            ReplyKeyboardMarkup superUserMainMenu,
            ReplyKeyboardMarkup adminMainMenu) {
        EnumMap<Menu, ReplyKeyboardMarkup> menus = new EnumMap<>(Menu.class);
        menus.put(Menu.SELECTION_MENU, selectionMenu);
        menus.put(Menu.USER_MAIN_MENU, userMainMenu);
        menus.put(Menu.SUPERUSER_MAIN_MENU, superUserMainMenu);
        menus.put(Menu.ADMIN_MAIN_MENU, adminMainMenu);
        return menus;
    }

    @Bean("userMainMenu")
    public ReplyKeyboardMarkup getUserMainMenu() {
        return getBaseMenu();
    }

    @Bean("adminMainMenu")
    public ReplyKeyboardMarkup getAdminMainMenu() {
        ReplyKeyboardMarkup replyMarkup = getBaseMenu();
        List<KeyboardRow> keyboard = replyMarkup.getKeyboard();
        KeyboardRow row = new KeyboardRow();
        row.add("Добавить партнера");
        row.add("Добавить мероприятие");
        keyboard.add(row);
        return replyMarkup;
    }

    @Bean("superUserMainMenu")
    public ReplyKeyboardMarkup getSuperUserMainMenu() {
        ReplyKeyboardMarkup replyMarkup = getBaseMenu();
        List<KeyboardRow> keyboard = replyMarkup.getKeyboard();
        KeyboardRow row = new KeyboardRow();
        row.add("Назначить админа");
        row.add("Добавить партнера");
        row.add("Добавить мероприятие");
        keyboard.add(row);
        return replyMarkup;
    }

    @Bean("selectionMenu")
    public ReplyKeyboardMarkup getSelectionMenu() {
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

    @Bean
    public Map<Long, InteractionState> getInteractionStatesMap() {
        return new ConcurrentHashMap<>();
    }

    private ReplyKeyboardMarkup getBaseMenu() {
        ReplyKeyboardMarkup replyMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add("Партнеры");
        row.add("Мероприятия");
        row.add("Изменить профиль");
        keyboard.add(row);
        replyMarkup.setKeyboard(keyboard);
        replyMarkup.setResizeKeyboard(true);
        replyMarkup.setOneTimeKeyboard(false);
        return replyMarkup;
    }
}