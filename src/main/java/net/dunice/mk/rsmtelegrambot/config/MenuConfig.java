package net.dunice.mk.rsmtelegrambot.config;

import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.ADD_EVENT;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.ADD_PARTNER;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.EVENTS;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.NO;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.PARTNERS;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.RSM_MEMBER;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.RSM_PARTNER;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.SET_ADMIN;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.TO_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.TRY_AGAIN;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.UPDATE_PROFILE;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.YES;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.ADMIN_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.GO_TO_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.SELECTION_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.SELECTION_USER_TYPE_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.SUPERUSER_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.TRY_AGAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.TRY_AGAIN_OR_GO_TO_MAIN_MENU;

import net.dunice.mk.rsmtelegrambot.constant.Menu;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

@Configuration
public class MenuConfig {

    @Bean
    public EnumMap<Menu, ReplyKeyboard> getReplyKeyboardMenus(
        ReplyKeyboard userMainMenu,
        ReplyKeyboard selectionMenu,
        ReplyKeyboard superUserMainMenu,
        ReplyKeyboard adminMainMenu,
        ReplyKeyboard goToMainMenu,
        ReplyKeyboard tryAgainMenu,
        ReplyKeyboard tryAgainOrGoToMainMenu,
        ReplyKeyboard selectionUserTypeMenu) {
        EnumMap<Menu, ReplyKeyboard> menus = new EnumMap<>(Menu.class);
        menus.put(SELECTION_MENU, selectionMenu);
        menus.put(SELECTION_USER_TYPE_MENU, selectionUserTypeMenu);
        menus.put(MAIN_MENU, userMainMenu);
        menus.put(SUPERUSER_MAIN_MENU, superUserMainMenu);
        menus.put(ADMIN_MAIN_MENU, adminMainMenu);
        menus.put(GO_TO_MAIN_MENU, goToMainMenu);
        menus.put(TRY_AGAIN_MENU, tryAgainMenu);
        menus.put(TRY_AGAIN_OR_GO_TO_MAIN_MENU, tryAgainOrGoToMainMenu);
        return menus;
    }

    @Bean("userMainMenu")
    public ReplyKeyboard getUserMainMenu() {
        return getBaseMenu();
    }

    @Bean("adminMainMenu")
    public ReplyKeyboard getAdminMainMenu() {
        ReplyKeyboardMarkup replyMarkup = (ReplyKeyboardMarkup)getBaseMenu();
        List<KeyboardRow> keyboard = replyMarkup.getKeyboard();
        KeyboardRow row = new KeyboardRow();
        row.add(ADD_PARTNER);
        row.add(ADD_EVENT);
        keyboard.add(row);
        return replyMarkup;
    }

    @Bean("superUserMainMenu")
    public ReplyKeyboard getSuperUserMainMenu() {
        ReplyKeyboardMarkup replyMarkup = (ReplyKeyboardMarkup)getBaseMenu();
        List<KeyboardRow> keyboard = replyMarkup.getKeyboard();
        KeyboardRow row = new KeyboardRow();
        row.add(SET_ADMIN);
        row.add(ADD_PARTNER);
        row.add(ADD_EVENT);
        keyboard.add(row);
        return replyMarkup;
    }

    @Bean("selectionMenu")
    public ReplyKeyboard getSelectionMenu() {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton buttonYes = new InlineKeyboardButton();
        buttonYes.setText(YES);
        buttonYes.setCallbackData(buttonYes.getText());
        InlineKeyboardButton buttonNo = new InlineKeyboardButton();
        buttonNo.setText(NO);
        buttonNo.setCallbackData(buttonNo.getText());
        row.add(buttonYes);
        row.add(buttonNo);
        keyboard.add(row);
        inlineKeyboardMarkup.setKeyboard(keyboard);
        return inlineKeyboardMarkup;
    }

    @Bean("selectionUserTypeMenu")
    public ReplyKeyboard getSelectionUserTypeMenu() {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton buttonMember = new InlineKeyboardButton();
        buttonMember.setText(RSM_MEMBER);
        buttonMember.setCallbackData(buttonMember.getText());
        InlineKeyboardButton buttonPartner = new InlineKeyboardButton();
        buttonPartner.setText(RSM_PARTNER);
        buttonPartner.setCallbackData(buttonPartner.getText());
        row.add(buttonMember);
        row.add(buttonPartner);
        keyboard.add(row);
        inlineKeyboardMarkup.setKeyboard(keyboard);
        return inlineKeyboardMarkup;
    }

    @Bean("tryAgainMenu")
    public ReplyKeyboard getTryAgainMenu() {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        InlineKeyboardButton tryAgainButton = new InlineKeyboardButton(TRY_AGAIN);
        tryAgainButton.setCallbackData(tryAgainButton.getText());
        keyboard.add(List.of(tryAgainButton));
        inlineKeyboardMarkup.setKeyboard(keyboard);
        return inlineKeyboardMarkup;
    }

    @Bean("goToMainMenu")
    public ReplyKeyboard getGoToMainMenu() {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        InlineKeyboardButton mainMenuButton = new InlineKeyboardButton(TO_MAIN_MENU);
        mainMenuButton.setCallbackData(mainMenuButton.getText());
        keyboard.add(List.of(mainMenuButton));
        inlineKeyboardMarkup.setKeyboard(keyboard);
        return inlineKeyboardMarkup;
    }

    @Bean("tryAgainOrGoToMainMenu")
    public ReplyKeyboard getTryAgainOrGoToMainMenu() {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        InlineKeyboardButton mainMenuButton = new InlineKeyboardButton(TO_MAIN_MENU);
        mainMenuButton.setCallbackData(mainMenuButton.getText());
        InlineKeyboardButton tryAgainButton = new InlineKeyboardButton(TRY_AGAIN);
        tryAgainButton.setCallbackData(tryAgainButton.getText());
        keyboard.add(List.of(mainMenuButton));
        keyboard.add(List.of(tryAgainButton));
        inlineKeyboardMarkup.setKeyboard(keyboard);
        return inlineKeyboardMarkup;
    }


    private ReplyKeyboard getBaseMenu() {
        ReplyKeyboardMarkup replyMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add(PARTNERS);
        row.add(EVENTS);
        row.add(UPDATE_PROFILE);
        keyboard.add(row);
        replyMarkup.setKeyboard(keyboard);
        replyMarkup.setResizeKeyboard(true);
        replyMarkup.setOneTimeKeyboard(false);
        return replyMarkup;
    }
}