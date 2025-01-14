package net.dunice.mk.rsmtelegrambot.handler.messagehandler;

import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.CANCEL;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.CHANGE_DISCOUNT;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.PARTNERS_LIST;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.TO_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.CANCEL_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.GO_TO_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.SELECTION_MENU;
import static net.dunice.mk.rsmtelegrambot.entity.Role.USER;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.BasicStep;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.BasicStep.IN_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.BasicStep.IN_PARTNER_MENU;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.BasicStep.SHOW_PARTNERS;
import static net.dunice.mk.rsmtelegrambot.handler.state.ShowPartnersState.ShowPartnersStep.CONFIRM_CHANGE;
import static net.dunice.mk.rsmtelegrambot.handler.state.ShowPartnersState.ShowPartnersStep.HANDLE_USER_ACTION;
import static net.dunice.mk.rsmtelegrambot.handler.state.ShowPartnersState.ShowPartnersStep.SHOW_PARTNERS_LIST;
import static net.dunice.mk.rsmtelegrambot.handler.state.ShowPartnersState.ShowPartnersStep.SHOW_PARTNER_DETAILS;
import static net.dunice.mk.rsmtelegrambot.handler.state.ShowPartnersState.ShowPartnersStep.VERIFY_NEW_DISCOUNT_DATE;
import static net.dunice.mk.rsmtelegrambot.handler.state.ShowPartnersState.ShowPartnersStep.VERIFY_NEW_DISCOUNT_PERCENT;

import lombok.RequiredArgsConstructor;
import net.dunice.mk.rsmtelegrambot.constant.Menu;
import net.dunice.mk.rsmtelegrambot.dto.MessageDto;
import net.dunice.mk.rsmtelegrambot.entity.Partner;
import net.dunice.mk.rsmtelegrambot.entity.User;
import net.dunice.mk.rsmtelegrambot.handler.MenuGenerator;
import net.dunice.mk.rsmtelegrambot.handler.state.BasicState;
import net.dunice.mk.rsmtelegrambot.handler.state.ShowPartnersState;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ShowPartnersHandler implements MessageHandler {

    private static final String PARTNER_INFO_FOR_USERS = """
        Партнер: %s
        Категория: %s
        Информация о партнере: %s
        Номер телефона: %s
        Процент скидки: %s%%
        Дата окончания скидки: %s
        """;
    private static final String PARTNER_INFO_FOR_PARTNERS = """
        Партнер: %s
        Категория: %s
        Информация о партнере: %s
        Номер телефона: %s
        """;
    private static final String CHANGE_DISCOUNT_INFO = """
        Хотите изменить данные у партнера на следующие?
        Партнер: %s
        Процент скидки: %s%%
        Дата окончания скидки: %s
        """;
    private final PartnerRepository partnerRepository;
    private final MenuGenerator menuGenerator;
    private final Map<Menu, ReplyKeyboard> menus;
    private final Map<Long, BasicState> basicStates;
    private final UserRepository userRepository;
    private final Map<Long, ShowPartnersState> showPartnersStates;

    @Override
    public BasicStep getStep() {
        return SHOW_PARTNERS;
    }

    @Override
    public PartialBotApiMethod<Message> handle(MessageDto messageDto, Long telegramId) {
        String text = messageDto.getText();
        ShowPartnersState state = showPartnersStates.get(telegramId);
        if (state == null) {
            showPartnersStates.put(telegramId, (state = new ShowPartnersState()));
        }
        if (StringUtils.equalsAny(text, TO_MAIN_MENU, CANCEL)) {
            return goToMainMenu(telegramId);
        }
        return switch (state.getStep()) {
            case SHOW_PARTNERS_LIST -> {
                List<Partner> partners = partnerRepository.findValidPartnersWithPresentDiscount();
                state.setStep(SHOW_PARTNER_DETAILS);
                yield generateSendMessage(telegramId, "Партнеры РСМ: ",
                    getPartnersListKeyboard(partners));
            }
            case SHOW_PARTNER_DETAILS -> {
                Optional<Partner> partnerOptional = partnerRepository.findByName(text);
                if (partnerOptional.isPresent()) {
                    Partner targetPartner = partnerOptional.get();
                    Optional<User> userOptional = userRepository.findById(telegramId);
                    String partnerDescription;
                    if (userOptional.isEmpty()) {
                        partnerDescription = PARTNER_INFO_FOR_PARTNERS.formatted(
                            targetPartner.getName(),
                            targetPartner.getCategory().getCategoryName(),
                            targetPartner.getPartnersInfo(),
                            targetPartner.getPhoneNumber());
                    } else {
                        partnerDescription = PARTNER_INFO_FOR_USERS.formatted(
                            targetPartner.getName(),
                            targetPartner.getCategory().getCategoryName(),
                            targetPartner.getPartnersInfo(),
                            targetPartner.getPhoneNumber(),
                            targetPartner.getDiscountPercent(),
                            targetPartner.getDiscountDate() == null ? "Неограниченно" :
                                targetPartner.getDiscountDate().toLocalDate());
                    }
                    byte[] logo = targetPartner.getLogo();
                    state.setStep(HANDLE_USER_ACTION);
                    state.setTargetPartner(targetPartner);
                    yield isLogoPresent(logo)
                        ?
                        generateImageMessage(telegramId, partnerDescription, getUserActionKeyboard(userOptional), logo)
                        : generateSendMessage(telegramId, partnerDescription, getUserActionKeyboard(userOptional));
                } else {
                    yield generateSendMessage(telegramId, "Нет партнера с таким названием.");
                }
            }
            case HANDLE_USER_ACTION -> {
                if (PARTNERS_LIST.equalsIgnoreCase(text)) {
                    state.setStep(SHOW_PARTNERS_LIST);
                    yield handle(messageDto, telegramId);
                } else if (CHANGE_DISCOUNT.equalsIgnoreCase(text) &&
                           userRepository.findById(telegramId).get().getUserRole() != USER) {
                    state.setStep(VERIFY_NEW_DISCOUNT_PERCENT);
                    yield generateSendMessage(telegramId, "Пожалуйста, введите новый процент скидки (от 0 до 100):",
                        menus.get(CANCEL_MENU));
                } else {
                    yield goToMainMenu(telegramId);
                }
            }
            case VERIFY_NEW_DISCOUNT_PERCENT -> {
                try {
                    Short discountPercent = Short.parseShort(text);
                    if (discountPercent >= 100 || discountPercent < 0) {
                        yield generateSendMessage(telegramId,
                            "Процент скидки должен быть от 0 до 100. Повторите ввод:", menus.get(CANCEL_MENU));
                    }
                    state.setDiscountPercent(discountPercent);
                    state.setStep(VERIFY_NEW_DISCOUNT_DATE);
                    yield generateSendMessage(telegramId,
                        "Пожалуйста, введите дату конца действия скидки в формате (ДД.ММ.ГГГГ-ЧЧ:ММ)",
                        menus.get(CANCEL_MENU));
                } catch (NumberFormatException e) {
                    yield generateSendMessage(telegramId, "Процент скидки должен быть числом. Повторите ввод:",
                        menus.get(CANCEL_MENU));
                }
            }
            case VERIFY_NEW_DISCOUNT_DATE -> {
                try {
                    state.setDiscountDate(
                        LocalDateTime.parse(text.trim(), DateTimeFormatter.ofPattern("dd.MM.yyyy-HH:mm")));
                    state.setStep(CONFIRM_CHANGE);
                    yield generateSendMessage(telegramId,
                        CHANGE_DISCOUNT_INFO.formatted(state.getTargetPartner().getName(),
                            state.getDiscountPercent(),
                            state.getDiscountDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy | HH:mm"))),
                        menus.get(SELECTION_MENU));
                } catch (DateTimeParseException e) {
                    yield generateSendMessage(telegramId,
                        "Дата должна быть в формате (ДД.ММ.ГГГГ-ЧЧ:ММ). Повторите ввод:", menus.get(CANCEL_MENU));
                }
            }
            case CONFIRM_CHANGE -> {
                String responseMessage;
                if ("Да".equalsIgnoreCase(text)) {
                    Partner currentPartner = state.getTargetPartner();
                    currentPartner.setDiscountPercent(state.getDiscountPercent());
                    currentPartner.setDiscountDate(state.getDiscountDate());
                    partnerRepository.save(currentPartner);
                    responseMessage = "Данные партнера успешно изменены.";
                } else if ("Нет".equalsIgnoreCase(text)) {
                    responseMessage = "Изменение данных партнера отменено.";
                } else {
                    responseMessage = "Неверная команда.";
                }
                yield generateSendMessage(telegramId, responseMessage,
                    menus.get(GO_TO_MAIN_MENU));
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

    private ReplyKeyboard getUserActionKeyboard(Optional<User> userOptional) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        InlineKeyboardButton toMainMenuButton = new InlineKeyboardButton(TO_MAIN_MENU);
        toMainMenuButton.setCallbackData(toMainMenuButton.getText());
        InlineKeyboardButton toPartnersButton = new InlineKeyboardButton(PARTNERS_LIST);
        toPartnersButton.setCallbackData(toPartnersButton.getText());
        keyboard.add(List.of(toMainMenuButton));
        keyboard.add(List.of(toPartnersButton));
        if (userOptional.isPresent() && userOptional.get().getUserRole() != USER) {
            InlineKeyboardButton changeDiscountButton = new InlineKeyboardButton(CHANGE_DISCOUNT);
            changeDiscountButton.setCallbackData(changeDiscountButton.getText());
            keyboard.add(List.of(changeDiscountButton));
        }
        inlineKeyboardMarkup.setKeyboard(keyboard);
        return inlineKeyboardMarkup;
    }

    private SendMessage goToMainMenu(Long telegramId) {
        showPartnersStates.remove(telegramId);
        if (isTgUserPartner(telegramId)) {
            basicStates.get(telegramId).setStep(IN_PARTNER_MENU);
            return generateSendMessage(telegramId, "Выберите раздел:", menus.get(Menu.PARTNER_MAIN_MENU));
        } else {
            basicStates.get(telegramId).setStep(IN_MAIN_MENU);
            return menuGenerator.generateRoleSpecificMainMenu(telegramId,
                userRepository.findByTelegramId(telegramId).get().getUserRole());
        }
    }

    private boolean isLogoPresent(byte[] logo) {
        return logo != null && logo.length > 0;
    }

    private boolean isTgUserPartner(Long telegramId) {
        return partnerRepository.existsById(telegramId);
    }
}