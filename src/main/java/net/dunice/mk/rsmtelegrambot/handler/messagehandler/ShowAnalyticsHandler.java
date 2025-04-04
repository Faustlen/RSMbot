package net.dunice.mk.rsmtelegrambot.handler.messagehandler;

import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.CANCEL;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.TO_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.BasicStep;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.BasicStep.IN_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.BasicStep.IN_PARTNER_MENU;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.BasicStep.SHOW_ANALYTICS;
import static net.dunice.mk.rsmtelegrambot.handler.state.ShowAnalyticsState.ShowAnalyticsStep.REQUEST_START_DATE;
import static net.dunice.mk.rsmtelegrambot.handler.state.ShowAnalyticsState.ShowAnalyticsStep.SELECT_PARTNER;

import lombok.RequiredArgsConstructor;
import net.dunice.mk.rsmtelegrambot.config.MenuConfig;
import net.dunice.mk.rsmtelegrambot.constant.Menu;
import net.dunice.mk.rsmtelegrambot.dto.MessageDto;
import net.dunice.mk.rsmtelegrambot.entity.Partner;
import net.dunice.mk.rsmtelegrambot.handler.MenuGenerator;
import net.dunice.mk.rsmtelegrambot.handler.state.BasicState;
import net.dunice.mk.rsmtelegrambot.handler.state.ShowAnalyticsState;
import net.dunice.mk.rsmtelegrambot.repository.CheckRepository;
import net.dunice.mk.rsmtelegrambot.repository.PartnerRepository;
import net.dunice.mk.rsmtelegrambot.repository.UserRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ShowAnalyticsHandler implements MessageHandler {

    private final PartnerRepository partnerRepository;
    private final MenuGenerator menuGenerator;
    private final MenuConfig menuConfig;
    private final Map<Menu, ReplyKeyboard> menus;
    private final UserRepository userRepository;
    private final Map<Long, BasicState> basicStates;
    private final CheckRepository checkRepository;
    private final Map<Long, ShowAnalyticsState> showAnalyticsStates;

    @Override
    public BasicStep getStep() {
        return SHOW_ANALYTICS;
    }

    @Override
    public PartialBotApiMethod<Message> handle(MessageDto messageDto, Long telegramId) {
        String text = messageDto.getText();

        ShowAnalyticsState state = showAnalyticsStates.get(telegramId);
        if (state == null) {
            state = new ShowAnalyticsState();
            showAnalyticsStates.put(telegramId, state);

            if (partnerRepository.existsById(telegramId)) {
                state.setStep(REQUEST_START_DATE);
                state.setSelectedPartnerId(telegramId);
                return generateSendMessage(
                    telegramId,
                    "Введите дату начала расчета аналитики в формате ДД.ММ.ГГГГ:",
                    menuConfig.getGoToMainMenu()
                );
            } else {
                state.setStep(ShowAnalyticsState.ShowAnalyticsStep.SHOW_PARTNERS_LIST);
            }
        }

        if (StringUtils.equalsAny(text, TO_MAIN_MENU, CANCEL)) {
            return goToMainMenu(telegramId);
        }

        return switch (state.getStep()) {
            case SHOW_PARTNERS_LIST -> handleShowPartnersList(telegramId, state);
            case SELECT_PARTNER     -> handleSelectPartner(telegramId, state, text);
            case REQUEST_START_DATE -> handleRequestStartDate(telegramId, state, text);
            case REQUEST_END_DATE   -> handleRequestEndDate(telegramId, state, text);
        };
    }

    private PartialBotApiMethod<Message> handleShowPartnersList(Long telegramId, ShowAnalyticsState state) {
        List<Partner> partners = partnerRepository.findAll();
        state.setStep(SELECT_PARTNER);

        return generateSendMessage(
            telegramId,
            "Выберите партнёра:",
            menuConfig.getPartnersListKeyboard(partners, false)
        );
    }

    private PartialBotApiMethod<Message> handleSelectPartner(Long telegramId, ShowAnalyticsState state, String text) {
        if (text == null) {
            List<Partner> partners = partnerRepository.findAll();
            return generateSendMessage(
                telegramId,
                "Выберите партнёра:",
                menuConfig.getPartnersListKeyboard(partners, false)
            );
        }

        if (text.endsWith("✅") || text.endsWith("❌")) {
            text = text.substring(0, text.length() - 2).trim();
        }

        Partner selectedPartner = partnerRepository.findByName(text).orElse(null);
        if (selectedPartner == null) {
            return generateSendMessage(
                telegramId,
                "Партнёр не найден.",
                menuConfig.getGoToMainMenu()
            );
        }

        state.setSelectedPartnerId(selectedPartner.getPartnerTelegramId());
        state.setStep(REQUEST_START_DATE);

        return generateSendMessage(
            telegramId,
            "Введите дату начала расчета аналитики в формате ДД.ММ.ГГГГ:",
            menuConfig.getGoToMainMenu()
        );
    }

    private PartialBotApiMethod<Message> handleRequestStartDate(Long telegramId, ShowAnalyticsState state, String text) {
        try {
            state.setStartDate(LocalDate.parse(text, DateTimeFormatter.ofPattern("dd.MM.yyyy")).atStartOfDay());
            state.setStep(ShowAnalyticsState.ShowAnalyticsStep.REQUEST_END_DATE);

            return generateSendMessage(
                telegramId,
                "Введите дату конца расчета аналитики в формате ДД.ММ.ГГГГ:",
                menuConfig.getGoToMainMenu()
            );
        } catch (Exception e) {
            return generateSendMessage(
                telegramId,
                "Неверный формат даты. (ДД.ММ.ГГГГ)",
                menuConfig.getGoToMainMenu()
            );
        }
    }

    private PartialBotApiMethod<Message> handleRequestEndDate(Long telegramId, ShowAnalyticsState state, String text) {
        try {
            state.setEndDate(
                LocalDate.parse(text, DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                    .atTime(23, 59, 59, 999999999)
            );

            var analytics = checkRepository.getAnalytics(
                state.getSelectedPartnerId(),
                state.getStartDate(),
                state.getEndDate()
            );

            if (analytics.getCheckCount() == 0) {
                return generateSendMessage(
                    telegramId,
                    "На данный период чеков не найдено",
                    menuConfig.getGoToMainMenu()
                );
            } else {
                String msg = "Аналитика:\n" +
                    "Средний чек: " + Math.round(analytics.getAvgCheck()) + " р.\n" +
                    "Сумма чеков: " + Math.round(analytics.getTotalSum()) + " р.\n" +
                    "Сумма скидок: " + Math.round(analytics.getTotalDiscount()) + " р.\n" +
                    "Количество чеков: " + analytics.getCheckCount();

                return generateSendMessage(
                    telegramId,
                    msg,
                    menuConfig.getGoToMainMenu()
                );
            }
        } catch (Exception e) {
            return generateSendMessage(
                telegramId,
                "Ошибка при обработке данных. (ДД.ММ.ГГГГ)",
                menuConfig.getGoToMainMenu()
            );
        }
    }

    private SendMessage goToMainMenu(Long telegramId) {
        showAnalyticsStates.remove(telegramId);

        if (partnerRepository.existsById(telegramId)) {
            basicStates.get(telegramId).setStep(IN_PARTNER_MENU);
            Partner partner = partnerRepository.findById(telegramId).get();
            return generateSendMessage(
                telegramId,
                "Выберите раздел:",
                menuGenerator.getPartnerMenu(partner)
            );
        } else {
            basicStates.get(telegramId).setStep(IN_MAIN_MENU);
            return menuGenerator.generateRoleSpecificMainMenu(
                telegramId,
                userRepository.findByTelegramId(telegramId).get().getUserRole()
            );
        }
    }
}
