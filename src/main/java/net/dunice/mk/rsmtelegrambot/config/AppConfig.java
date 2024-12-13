package net.dunice.mk.rsmtelegrambot.config;

import net.dunice.mk.rsmtelegrambot.handler.state.BasicState;

import static net.dunice.mk.rsmtelegrambot.constant.Menu.*;

import net.dunice.mk.rsmtelegrambot.constant.Menu;
import net.dunice.mk.rsmtelegrambot.handler.state.stateobject.GrantAdminState;
import net.dunice.mk.rsmtelegrambot.handler.state.stateobject.RegistrationState;
import net.dunice.mk.rsmtelegrambot.handler.state.stateobject.UpdateProfileState;
import net.dunice.mk.rsmtelegrambot.handler.state.stateobject.step.ShowEventsStep;
import net.dunice.mk.rsmtelegrambot.handler.state.stateobject.step.UpdateProfileStep;
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
        menus.put(SELECTION_MENU, selectionMenu);
        menus.put(MAIN_MENU, userMainMenu);
        menus.put(SUPERUSER_MAIN_MENU, superUserMainMenu);
        menus.put(ADMIN_MAIN_MENU, adminMainMenu);
        return menus;
    }

    @Bean
    public Map<Long, BasicState> getInteractionStatesMap() {
        return new ConcurrentHashMap<>();
    }

    @Bean("grantAdminStates")
    public Map<Long, GrantAdminState> getGrantAdminStatesMap() {
        return new ConcurrentHashMap<>();
    }

    @Bean("registrationStates")
    public Map<Long, RegistrationState> getRegistrationStatesMap() {
        return new ConcurrentHashMap<>();
    }

    @Bean("updateProfileStates")
    public Map<Long, UpdateProfileState> getUpdateProfileStatesMap() {
        return new ConcurrentHashMap<>();
    }

    @Bean("showEventSteps")
    public Map<Long, ShowEventsStep> showEventsStepMap() {
        return new ConcurrentHashMap<>();
    }

    @Bean("allStatesMap")
    public List<Map<Long, ?>> getAllStatesMap(Map<Long, GrantAdminState> grantAdminStates,
                                              Map<Long, RegistrationState> registrationStates,
                                              Map<Long, UpdateProfileState> updateProfileStates,
                                              Map<Long, ShowEventsStep> showEventSteps) {
        return List.of(grantAdminStates, registrationStates, updateProfileStates, showEventSteps);
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