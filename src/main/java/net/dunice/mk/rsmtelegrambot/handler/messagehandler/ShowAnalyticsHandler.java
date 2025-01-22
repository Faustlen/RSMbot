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
            showAnalyticsStates.put(telegramId, state = new ShowAnalyticsState());
            if (partnerRepository.existsById(telegramId)) {
                state.setStep(REQUEST_START_DATE);
                state.setSelectedPartnerId(telegramId);
                return generateSendMessage(telegramId, "Введите дату начала расчета аналитики в формате ДД.ММ.ГГГГ:",
                    menuConfig.getGoToMainMenu());
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
                    menuConfig.getPartnersListKeyboard(partners));
            }
            case SELECT_PARTNER -> {

                Partner selectedPartner = partnerRepository.findByName(text).orElse(null);
                if (selectedPartner == null) {
                    yield generateSendMessage(telegramId, "Партнёр не найден.",
                        menuConfig.getGoToMainMenu());
                }
                state.setSelectedPartnerId(selectedPartner.getPartnerTelegramId());
                state.setStep(ShowAnalyticsState.ShowAnalyticsStep.REQUEST_START_DATE);
                yield generateSendMessage(telegramId, "Введите дату начала расчета аналитики в формате ДД.ММ.ГГГГ:",
                    menuConfig.getGoToMainMenu());
            }
            case REQUEST_START_DATE -> {
                try {
                    state.setStartDate(LocalDate.parse(text, DateTimeFormatter.ofPattern("dd.MM.yyyy")).atStartOfDay());
                    state.setStep(ShowAnalyticsState.ShowAnalyticsStep.REQUEST_END_DATE);
                    yield generateSendMessage(telegramId, "Введите дату конца расчета аналитики в формате ДД.ММ.ГГГГ:",
                        menuConfig.getGoToMainMenu());
                } catch (Exception e) {
                    yield generateSendMessage(telegramId, "Неверный формат даты. (ДД.ММ.ГГГГ)",
                        menuConfig.getGoToMainMenu());
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
                    if (analytics.getCheckCount() == 0) {
                        yield generateSendMessage(telegramId, "На данный период чеков не найдено",
                            menuConfig.getGoToMainMenu());
                    } else {
                        yield generateSendMessage(telegramId,
                            "Аналитика:\n" +
                            "Средний чек: " + analytics.getAvgCheck() + "\n" +
                            "Сумма чеков: " + analytics.getTotalSum() + "\n" +
                            "Сумма скидок: " + analytics.getTotalDiscount() + "\n" +
                            "Количество чеков: " + analytics.getCheckCount(),
                            menuConfig.getGoToMainMenu());
                    }
                } catch (Exception e) {
                    yield generateSendMessage(telegramId, "Ошибка при обработке данных. (ДД.ММ.ГГГГ)",
                        menuConfig.getGoToMainMenu());
                }
            }
        };
    }

    private SendMessage goToMainMenu(Long telegramId) {
        showAnalyticsStates.remove(telegramId);
        if (partnerRepository.existsById(telegramId)) {
            basicStates.get(telegramId).setStep(IN_PARTNER_MENU);
            return generateSendMessage(telegramId, "Выберите раздел:", menus.get(Menu.PARTNER_MAIN_MENU));
        } else {
            basicStates.get(telegramId).setStep(IN_MAIN_MENU);
            return menuGenerator.generateRoleSpecificMainMenu(telegramId,
                userRepository.findByTelegramId(telegramId).get().getUserRole());
        }
    }
}
