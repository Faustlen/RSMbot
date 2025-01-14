package net.dunice.mk.rsmtelegrambot.handler.messagehandler;

import lombok.RequiredArgsConstructor;
import net.dunice.mk.rsmtelegrambot.constant.Menu;
import net.dunice.mk.rsmtelegrambot.dto.MessageDto;
import net.dunice.mk.rsmtelegrambot.entity.Partner;
import net.dunice.mk.rsmtelegrambot.handler.MenuGenerator;
import net.dunice.mk.rsmtelegrambot.handler.state.BasicState;
import net.dunice.mk.rsmtelegrambot.handler.state.stateobject.ShowAnalyticsState;
import net.dunice.mk.rsmtelegrambot.repository.CheckRepository;
import net.dunice.mk.rsmtelegrambot.repository.PartnerRepository;
import net.dunice.mk.rsmtelegrambot.repository.UserRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.CANCEL;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.TO_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.IN_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.IN_PARTNER_MENU;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.SHOW_ANALYTICS;
import static net.dunice.mk.rsmtelegrambot.handler.state.stateobject.ShowAnalyticsState.ShowAnalyticsStep.REQUEST_START_DATE;
import static net.dunice.mk.rsmtelegrambot.handler.state.stateobject.ShowAnalyticsState.ShowAnalyticsStep.SELECT_PARTNER;


@Service
@RequiredArgsConstructor
public class ShowAnalyticsHandler implements MessageHandler {

    private final PartnerRepository partnerRepository;
    private final MenuGenerator menuGenerator;
    private final Map<Menu, ReplyKeyboard> menus;
    private final UserRepository userRepository;
    private final Map<Long, BasicState> basicStates;
    private final CheckRepository checkRepository;
    private final Map<Long, ShowAnalyticsState> showAnalyticsStates;

    @Override
    public BasicState getState() {
        return SHOW_ANALYTICS;
    }

    @Override
    public PartialBotApiMethod<Message> handle(MessageDto messageDto, Long telegramId) {
        String text = messageDto.getText();
        ShowAnalyticsState state = showAnalyticsStates.get(telegramId);
        if (state == null) {
            showAnalyticsStates.put(telegramId, (state = new ShowAnalyticsState()));
            if (partnerRepository.existsById(telegramId)) {
                state.setStep(REQUEST_START_DATE);
                state.setSelectedPartnerId(telegramId);
                return generateSendMessage(telegramId, "Введите дату начала расчета аналитики в формате ДД.ММ.ГГГГ:",
                        getUserActionKeyboard(telegramId));
            }
        }
        if (StringUtils.equalsAny(text, TO_MAIN_MENU, CANCEL)) {
            return goToMainMenu(telegramId);
        }

        return switch (state.getStep()) {
            case SHOW_PARTNERS_LIST -> {
                List<Partner> partners = partnerRepository.findValidPartnersWithPresentDiscount();
                state.setStep(SELECT_PARTNER);
                yield generateSendMessage(telegramId, "Выберите партнёра: ",
                        getPartnersListKeyboard(partners));
            }
            case SELECT_PARTNER -> {

                Partner selectedPartner = partnerRepository.findByName(text).orElse(null);
                if (selectedPartner == null) {
                    yield generateSendMessage(telegramId, "Партнёр не найден.",
                            getUserActionKeyboard(telegramId));
                }
                state.setSelectedPartnerId(selectedPartner.getPartnerTelegramId());
                state.setStep(ShowAnalyticsState.ShowAnalyticsStep.REQUEST_START_DATE);
                yield generateSendMessage(telegramId, "Введите дату начала расчета аналитики в формате ДД.ММ.ГГГГ:",
                        getUserActionKeyboard(telegramId));
            }
            case REQUEST_START_DATE -> {
                try {
                    state.setStartDate(LocalDate.parse(text, DateTimeFormatter.ofPattern("dd.MM.yyyy")).atStartOfDay());
                    state.setStep(ShowAnalyticsState.ShowAnalyticsStep.REQUEST_END_DATE);
                    yield generateSendMessage(telegramId, "Введите дату конца расчета аналитики в формате ДД.ММ.ГГГГ:",
                            getUserActionKeyboard(telegramId));
                } catch (Exception e) {
                    yield generateSendMessage(telegramId, "Неверный формат даты. (ДД.ММ.ГГГГ)",
                            getUserActionKeyboard(telegramId));
                }
            }
            case REQUEST_END_DATE -> {
                try {
                    state.setEndDate(LocalDate.parse(text, DateTimeFormatter.ofPattern("dd.MM.yyyy")).atStartOfDay());
                    var analytics = checkRepository.getAnalytics(
                            state.getSelectedPartnerId(),
                            state.getStartDate(),
                            state.getEndDate()
                    );
                    yield generateSendMessage(telegramId,
                            "Аналитика:\n" +
                                    "Средний чек: " + analytics.getAvgCheck() + "\n" +
                                    "Сумма чеков: " + analytics.getTotalSum() + "\n" +
                                    "Сумма скидок: " + analytics.getTotalDiscount() + "\n" +
                                    "Количество чеков: " + analytics.getCheckCount(), getUserActionKeyboard(telegramId));
                } catch (Exception e) {
                    yield generateSendMessage(telegramId, "Ошибка при обработке данных. (ДД.ММ.ГГГГ)",
                            getUserActionKeyboard(telegramId));
                }
            }
        };
    }

    private ReplyKeyboardMarkup getPartnersListKeyboard(List<Partner> partners) {
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

    private ReplyKeyboard getUserActionKeyboard(Long telegramId) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        InlineKeyboardButton toMainMenuButton = new InlineKeyboardButton(TO_MAIN_MENU);
        toMainMenuButton.setCallbackData(toMainMenuButton.getText());
        keyboard.add(List.of(toMainMenuButton));
        inlineKeyboardMarkup.setKeyboard(keyboard);
        return inlineKeyboardMarkup;
    }

    private SendMessage goToMainMenu(Long telegramId) {
        showAnalyticsStates.remove(telegramId);
        if (partnerRepository.existsById(telegramId)) {
            basicStates.put(telegramId, IN_PARTNER_MENU);
            return generateSendMessage(telegramId, "Партнеры РСМ:", menus.get(Menu.PARTNER_MAIN_MENU));
        } else {
            basicStates.put(telegramId, IN_MAIN_MENU);
            return menuGenerator.generateRoleSpecificMainMenu(telegramId,
                    userRepository.findByTelegramId(telegramId).get().getUserRole());
        }
    }
}
