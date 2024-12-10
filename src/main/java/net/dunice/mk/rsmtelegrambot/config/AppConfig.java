package net.dunice.mk.rsmtelegrambot.config;

import net.dunice.mk.rsmtelegrambot.constants.InteractionState;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import static net.dunice.mk.rsmtelegrambot.constants.InteractionState.*;

@Configuration
public class AppConfig {

    @Bean
    public EnumMap<InteractionState, InlineKeyboardMarkup> getKeyboardMenus (
            InlineKeyboardMarkup userMainMenu, InlineKeyboardMarkup registrationMenu,
            InlineKeyboardMarkup superUserMainMenu, InlineKeyboardMarkup adminMainMenu) {
        EnumMap<InteractionState, InlineKeyboardMarkup> menus = new EnumMap<>(InteractionState.class);
        menus.put(REGISTRATION, registrationMenu);
        menus.put(USER_MAIN_MENU, userMainMenu);
        menus.put(SUPER_USER_MAIN_MENU, superUserMainMenu);
        menus.put(ADMIN_MAIN_MENU, adminMainMenu);
        return menus;
    }

    @Bean("userMainMenu")
    public InlineKeyboardMarkup getUserMainMenu() {
        return getBaseMenu();
    }

    @Bean("adminMainMenu")
    public InlineKeyboardMarkup getAdminMainMenu() {
        InlineKeyboardMarkup keyboardMarkup = getBaseMenu();
        List<List<InlineKeyboardButton>> keyboard = keyboardMarkup.getKeyboard();
        InlineKeyboardButton partnersButton = new InlineKeyboardButton("Добавить партнёра");
        partnersButton.setCallbackData("Добавить партнёра");
        InlineKeyboardButton eventsButton = new InlineKeyboardButton("Добавить мероприятие");
        eventsButton.setCallbackData("Добавить мероприятие");
        keyboard.add(List.of(partnersButton));
        keyboard.add(List.of(eventsButton));
        return keyboardMarkup;
    }

    @Bean("superUserMainMenu")
    public InlineKeyboardMarkup getSuperUserMainMenu() {
        InlineKeyboardMarkup keyboardMarkup = getBaseMenu();
        List<List<InlineKeyboardButton>> keyboard = keyboardMarkup.getKeyboard();
        InlineKeyboardButton setAdminButton = new InlineKeyboardButton("Назначить админа");
        setAdminButton.setCallbackData("Назначить админа");
        InlineKeyboardButton partnersButton = new InlineKeyboardButton("Добавить партнёра");
        partnersButton.setCallbackData("Добавить партнёра");
        InlineKeyboardButton eventsButton = new InlineKeyboardButton("Добавить мероприятие");
        eventsButton.setCallbackData("Добавить мероприятие");
        keyboard.add(List.of(setAdminButton));
        keyboard.add(List.of(partnersButton));
        keyboard.add(List.of(eventsButton));
        return keyboardMarkup;
    }

    @Bean("registrationMenu")
    public InlineKeyboardMarkup getRegistrationMenu() {
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        InlineKeyboardButton yesButton = new InlineKeyboardButton("Да");
        yesButton.setCallbackData("Да");
        InlineKeyboardButton noButton = new InlineKeyboardButton("Нет");
        noButton.setCallbackData("Нет");
        keyboard.add(List.of(yesButton, noButton));
        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    private InlineKeyboardMarkup getBaseMenu() {
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        InlineKeyboardButton partnersButton = new InlineKeyboardButton("Партнёры");
        partnersButton.setCallbackData("Партнёры");
        InlineKeyboardButton eventsButton = new InlineKeyboardButton("Мероприятия");
        eventsButton.setCallbackData("Мероприятия");
        InlineKeyboardButton changeProfileButton = new InlineKeyboardButton("Изменить профиль");
        changeProfileButton.setCallbackData("Изменить профиль");
        keyboard.add(List.of(partnersButton));
        keyboard.add(List.of(eventsButton));
        keyboard.add(List.of(changeProfileButton));
        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }
}
