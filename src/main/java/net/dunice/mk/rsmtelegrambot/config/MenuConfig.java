package net.dunice.mk.rsmtelegrambot.config;

import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.ADD_EVENT;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.ADMINS_LIST;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.CANCEL;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.EVENTS_LIST;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.NEW_CHECK;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.NO;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.PARTNERS_LIST;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.PERIOD_ANALYTICS;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.RSM_MEMBER;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.RSM_PARTNER;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.SEND_MESSAGE_TO_EVERYONE;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.TO_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.TRY_AGAIN;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.UPDATE_DISCOUNT_CODE;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.UPDATE_PROFILE;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.USERS_LIST;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.VERIFICATION_CODE;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.YES;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.ADMIN_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.CANCEL_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.CATEGORY_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.EVENT_FIELDS_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.GO_TO_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.PARTNER_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.SELECTION_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.SELECTION_USER_TYPE_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.SUPERUSER_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.TRY_AGAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.TRY_AGAIN_OR_GO_TO_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.UPDATE_DISCOUNT_CODE_MENU;

import net.dunice.mk.rsmtelegrambot.constant.Menu;
import net.dunice.mk.rsmtelegrambot.entity.Partner;
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
import java.util.stream.Stream;

@Configuration
public class MenuConfig {

    @Bean
    public EnumMap<Menu, ReplyKeyboard> getReplyKeyboardMenus(
        ReplyKeyboard userMainMenu, ReplyKeyboard selectionMenu,
        ReplyKeyboard superUserMainMenu, ReplyKeyboard adminMainMenu,
        ReplyKeyboard goToMainMenu, ReplyKeyboard tryAgainMenu,
        ReplyKeyboard tryAgainOrGoToMainMenu, ReplyKeyboard selectionUserTypeMenu,
        ReplyKeyboard partnerMainMenu, ReplyKeyboard cancelMenu,
        ReplyKeyboard categoryMenu, ReplyKeyboard eventFieldsMenu,
        ReplyKeyboard updateDiscountCodeMenu) {
        EnumMap<Menu, ReplyKeyboard> menus = new EnumMap<>(Menu.class);
        menus.put(SELECTION_MENU, selectionMenu);
        menus.put(SELECTION_USER_TYPE_MENU, selectionUserTypeMenu);
        menus.put(MAIN_MENU, userMainMenu);
        menus.put(SUPERUSER_MAIN_MENU, superUserMainMenu);
        menus.put(ADMIN_MAIN_MENU, adminMainMenu);
        menus.put(PARTNER_MAIN_MENU, partnerMainMenu);
        menus.put(GO_TO_MAIN_MENU, goToMainMenu);
        menus.put(TRY_AGAIN_MENU, tryAgainMenu);
        menus.put(TRY_AGAIN_OR_GO_TO_MAIN_MENU, tryAgainOrGoToMainMenu);
        menus.put(CANCEL_MENU, cancelMenu);
        menus.put(CATEGORY_MENU, categoryMenu);
        menus.put(EVENT_FIELDS_MENU, eventFieldsMenu);
        menus.put(UPDATE_DISCOUNT_CODE_MENU, updateDiscountCodeMenu);
        return menus;
    }

    @Bean("userMainMenu")
    public ReplyKeyboard getUserMainMenu() {
        return getBaseUserMenu();
    }

    @Bean("adminMainMenu")
    public ReplyKeyboard getAdminMainMenu() {
        return getBaseAdminMenu();
    }

    @Bean("superUserMainMenu")
    public ReplyKeyboard getSuperUserMainMenu() {
        return getBaseSuperUserMenu();
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

    @Bean("categoryMenu")
    public ReplyKeyboard createCategoryMenu() {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        InlineKeyboardButton[] buttons = Stream.of("Красота", "Развлечения и отдых", "Спорт", "Образование",
                "Услуги", "Магазины", "Кафе, рестораны, общепит")
            .map(category -> {
                InlineKeyboardButton button = new InlineKeyboardButton(category);
                button.setCallbackData(button.getText());
                return button;
            })
            .toArray(InlineKeyboardButton[]::new);

        for (int i = 0; i < buttons.length; i++) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(buttons[i]);
            if (++i < buttons.length) {
                row.add(buttons[i]);
            }
            keyboard.add(row);
        }
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

    @Bean("cancelMenu")
    public ReplyKeyboard getCancelMenu() {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        InlineKeyboardButton cancelButton = new InlineKeyboardButton(CANCEL);
        cancelButton.setCallbackData(cancelButton.getText());
        keyboard.add(List.of(cancelButton));
        inlineKeyboardMarkup.setKeyboard(keyboard);
        return inlineKeyboardMarkup;
    }

    @Bean("updateDiscountCodeMenu")
    public ReplyKeyboard getUpdateDiscountCodeMenu() {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        InlineKeyboardButton updateCodeButton = new InlineKeyboardButton(UPDATE_DISCOUNT_CODE);
        updateCodeButton.setCallbackData(updateCodeButton.getText());
        InlineKeyboardButton mainMenuButton = new InlineKeyboardButton(TO_MAIN_MENU);
        mainMenuButton.setCallbackData(mainMenuButton.getText());
        keyboard.add(List.of(updateCodeButton, mainMenuButton));
        inlineKeyboardMarkup.setKeyboard(keyboard);
        return inlineKeyboardMarkup;
    }

    @Bean("partnerMainMenu")
    public ReplyKeyboard getPartnerMenu() {
        ReplyKeyboardMarkup replyMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();
        keyboard.add(new KeyboardRow());
        keyboard.get(0).addAll(List.of(
            PARTNERS_LIST,
            PERIOD_ANALYTICS,
            VERIFICATION_CODE,
            NEW_CHECK));
        replyMarkup.setKeyboard(keyboard);
        replyMarkup.setResizeKeyboard(true);
        replyMarkup.setOneTimeKeyboard(false);
        return replyMarkup;
    }

    @Bean("eventFieldsMenu")
    public ReplyKeyboard getEventFieldsMenu() {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        InlineKeyboardButton[] buttons = Stream.of("Название", "Дата и Время", "Описание", "Ссылка")
            .map(category -> {
                InlineKeyboardButton button = new InlineKeyboardButton(category);
                button.setCallbackData(button.getText());
                return button;
            })
            .toArray(InlineKeyboardButton[]::new);
        for (int i = 0; i < buttons.length; i++) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(buttons[i]);
            if (++i < buttons.length) {
                row.add(buttons[i]);
            }
            keyboard.add(row);
        }
        inlineKeyboardMarkup.setKeyboard(keyboard);
        return inlineKeyboardMarkup;
    }

    public ReplyKeyboardMarkup getPartnersListKeyboard(List<Partner> partners) {
        ReplyKeyboardMarkup replyMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow firstRow = new KeyboardRow();
        firstRow.add(TO_MAIN_MENU);
        keyboard.add(firstRow);
        for (Partner partner : partners) {
            KeyboardRow row = new KeyboardRow();
            row.add(partner.getName());
            keyboard.add(row);
        }
        replyMarkup.setKeyboard(keyboard);
        replyMarkup.setResizeKeyboard(true);
        replyMarkup.setOneTimeKeyboard(false);
        return replyMarkup;
    }

    private ReplyKeyboardMarkup getBaseUserMenu() {
        ReplyKeyboardMarkup replyMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();
        keyboard.add(new KeyboardRow());
        keyboard.get(0).addAll(List.of(
            PARTNERS_LIST,
            EVENTS_LIST,
            UPDATE_PROFILE));
        replyMarkup.setKeyboard(keyboard);
        replyMarkup.setResizeKeyboard(true);
        replyMarkup.setOneTimeKeyboard(false);
        return replyMarkup;
    }

    private ReplyKeyboardMarkup getBaseAdminMenu() {
        ReplyKeyboardMarkup replyMarkup = getBaseUserMenu();
        List<KeyboardRow> keyboard = replyMarkup.getKeyboard();
        keyboard.add(new KeyboardRow());
        keyboard.get(1).addAll(List.of(
            ADD_EVENT,
            USERS_LIST,
            SEND_MESSAGE_TO_EVERYONE));
        return replyMarkup;
    }

    private ReplyKeyboardMarkup getBaseSuperUserMenu() {
        ReplyKeyboardMarkup replyMarkup = getBaseAdminMenu();
        List<KeyboardRow> keyboard = replyMarkup.getKeyboard();
        keyboard.add(new KeyboardRow());
        keyboard.get(2).addAll(List.of(
            ADMINS_LIST,
            PERIOD_ANALYTICS));
        return replyMarkup;
    }


}