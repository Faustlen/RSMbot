package net.dunice.mk.rsmtelegrambot.handler.messagehandler;

import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.ACTIVATE_PARTNER;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.CANCEL;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.CHANGE_DISCOUNT;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.DEACTIVATE_PARTNER;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.GET_DISCOUNT_CODE;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.PARTNERS_LIST;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.TO_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.CANCEL_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.GO_TO_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.SELECTION_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.UPDATE_DISCOUNT_CODE_MENU;
import static net.dunice.mk.rsmtelegrambot.entity.Role.ADMIN;
import static net.dunice.mk.rsmtelegrambot.entity.Role.SUPER_USER;
import static net.dunice.mk.rsmtelegrambot.entity.Role.USER;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.BasicStep;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.BasicStep.IN_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.BasicStep.IN_PARTNER_MENU;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.BasicStep.SHOW_PARTNERS;
import static net.dunice.mk.rsmtelegrambot.handler.state.ShowPartnersState.ShowPartnersStep.CONFIRM_CHANGE;
import static net.dunice.mk.rsmtelegrambot.handler.state.ShowPartnersState.ShowPartnersStep.HANDLE_USER_ACTION;
import static net.dunice.mk.rsmtelegrambot.handler.state.ShowPartnersState.ShowPartnersStep.SEND_DISCOUNT_CODE;
import static net.dunice.mk.rsmtelegrambot.handler.state.ShowPartnersState.ShowPartnersStep.SHOW_PARTNERS_LIST;
import static net.dunice.mk.rsmtelegrambot.handler.state.ShowPartnersState.ShowPartnersStep.SHOW_PARTNER_DETAILS;
import static net.dunice.mk.rsmtelegrambot.handler.state.ShowPartnersState.ShowPartnersStep.VERIFY_NEW_DISCOUNT_DATE;
import static net.dunice.mk.rsmtelegrambot.handler.state.ShowPartnersState.ShowPartnersStep.VERIFY_NEW_DISCOUNT_PERCENT;

import lombok.RequiredArgsConstructor;
import net.dunice.mk.rsmtelegrambot.config.MenuConfig;
import net.dunice.mk.rsmtelegrambot.constant.Menu;
import net.dunice.mk.rsmtelegrambot.dto.MessageDto;
import net.dunice.mk.rsmtelegrambot.entity.Partner;
import net.dunice.mk.rsmtelegrambot.entity.User;
import net.dunice.mk.rsmtelegrambot.handler.MenuGenerator;
import net.dunice.mk.rsmtelegrambot.handler.state.BasicState;
import net.dunice.mk.rsmtelegrambot.handler.state.ShowPartnersState;
import net.dunice.mk.rsmtelegrambot.repository.PartnerRepository;
import net.dunice.mk.rsmtelegrambot.repository.UserRepository;
import net.dunice.mk.rsmtelegrambot.service.DiscountCodeService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

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

    private static final String PARTNER_INFO_FOR_ADMIN = """
        Партнер: %s
        Категория: %s
        Информация о партнере: %s
        Процент скидки: %s%%
        Дата окончания скидки: %s
        Номер телефона: %s
        Активирован: %s
        """;

    private final PartnerRepository partnerRepository;
    private final MenuGenerator menuGenerator;
    private final MenuConfig menuConfig;
    private final Map<Menu, ReplyKeyboard> menus;
    private final Map<Long, BasicState> basicStates;
    private final UserRepository userRepository;
    private final Map<Long, ShowPartnersState> showPartnersStates;
    private final DiscountCodeService discountCodeService;

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
                List<Partner> partners = partnerRepository.findAll().stream()
                    .filter(Partner::isValid)
                    .toList();
                state.setStep(SHOW_PARTNER_DETAILS);
                yield generateSendMessage(telegramId, "Партнеры РСМ: ",
                    menuConfig.getPartnersListKeyboard(partners));
            }

            case SHOW_PARTNER_DETAILS -> {
                Optional<Partner> partnerOptional = partnerRepository.findByName(text);
                if (partnerOptional.isPresent()) {
                    Partner targetPartner = partnerOptional.get();
                    Optional<User> userOptional = userRepository.findById(telegramId);
                    User targetUser = userOptional.get();

                    String partnerDescription = "";
                    if (userOptional.isEmpty() && targetPartner.isValid()) {
                        partnerDescription = PARTNER_INFO_FOR_PARTNERS.formatted(
                            targetPartner.getName(),
                            targetPartner.getCategory().getCategoryName(),
                            targetPartner.getPartnersInfo(),
                            targetPartner.getPhoneNumber());
                    } else if (targetUser.getUserRole().equals(USER) && targetPartner.isValid()) {
                        partnerDescription = PARTNER_INFO_FOR_USERS.formatted(
                            targetPartner.getName(),
                            targetPartner.getCategory().getCategoryName(),
                            targetPartner.getPartnersInfo(),
                            targetPartner.getPhoneNumber(),
                            targetPartner.getDiscountPercent(),
                            targetPartner.getDiscountDate() == null ? "Неограниченно" :
                                targetPartner.getDiscountDate().toLocalDate());
                    } else if (targetUser.getUserRole().equals(SUPER_USER)) {
                        if (targetPartner.isValid()) {
                            partnerDescription = PARTNER_INFO_FOR_ADMIN.formatted(
                                targetPartner.getName(),
                                targetPartner.getCategory().getCategoryName(),
                                targetPartner.getPartnersInfo(),
                                targetPartner.getDiscountPercent(),
                                targetPartner.getDiscountDate() == null ? "Неограниченно" :
                                    targetPartner.getDiscountDate().toLocalDate(),
                                targetPartner.getPhoneNumber(),
                                "Да");
                        } else {
                            partnerDescription = PARTNER_INFO_FOR_ADMIN.formatted(
                                targetPartner.getName(),
                                targetPartner.getCategory().getCategoryName(),
                                targetPartner.getPartnersInfo(),
                                targetPartner.getDiscountPercent(),
                                targetPartner.getDiscountDate() == null ? "Неограниченно" :
                                    targetPartner.getDiscountDate().toLocalDate(),
                                targetPartner.getPhoneNumber(),
                                "Нет"
                            );
                        }
                    } else if (targetUser.getUserRole().equals(ADMIN)) {
                        if (targetPartner.isValid()) {
                            partnerDescription = PARTNER_INFO_FOR_ADMIN.formatted(
                                targetPartner.getName(),
                                targetPartner.getCategory().getCategoryName(),
                                targetPartner.getPartnersInfo(),
                                targetPartner.getDiscountPercent(),
                                targetPartner.getDiscountDate() == null ? "Неограниченно" :
                                    targetPartner.getDiscountDate().toLocalDate(),
                                targetPartner.getPhoneNumber(),
                                "Да");
                        } else {
                            partnerDescription = PARTNER_INFO_FOR_ADMIN.formatted(
                                targetPartner.getName(),
                                targetPartner.getCategory().getCategoryName(),
                                targetPartner.getPartnersInfo(),
                                targetPartner.getDiscountPercent(),
                                targetPartner.getDiscountDate() == null ? "Неограниченно" :
                                    targetPartner.getDiscountDate().toLocalDate(),
                                targetPartner.getPhoneNumber(),
                                "Нет"
                            );
                        }
                    }

                    byte[] logo = targetPartner.getLogo();
                    state.setStep(HANDLE_USER_ACTION);
                    state.setTargetPartner(targetPartner);

                    if (isLogoPresent(logo)) {
                        yield generateImageMessage(telegramId, partnerDescription,
                            getUserActionKeyboard(userOptional, partnerOptional), logo);
                    } else {
                        yield generateSendMessage(telegramId, partnerDescription,
                            getUserActionKeyboard(userOptional, partnerOptional));
                    }
                } else {
                    yield generateSendMessage(telegramId, "Нет партнера с таким названием.");
                }
            }

            case HANDLE_USER_ACTION -> {
                if (PARTNERS_LIST.equalsIgnoreCase(text)) {
                    state.setStep(SHOW_PARTNERS_LIST);
                    yield handle(messageDto, telegramId);
                } else if (CHANGE_DISCOUNT.equalsIgnoreCase(text) &&
                           basicStates.get(telegramId).getUser().getUserRole() != USER) {
                    state.setStep(VERIFY_NEW_DISCOUNT_PERCENT);
                    yield generateSendMessage(telegramId, "Пожалуйста, введите новый процент скидки (от 0 до 100):",
                        menus.get(CANCEL_MENU));
                } else if (GET_DISCOUNT_CODE.equalsIgnoreCase(text)) {
                    state.setStep(SEND_DISCOUNT_CODE);
                    yield handle(messageDto, telegramId);
                } else if (ACTIVATE_PARTNER.equalsIgnoreCase(text)) {
                    Partner currentPartner = state.getTargetPartner();
                    currentPartner.setValid(true);
                    partnerRepository.save(currentPartner);
                    yield generateSendMessage(telegramId, "Партнёр успешно активирован.",
                        getUserActionKeyboard(userRepository.findById(telegramId),
                            Optional.ofNullable(state.getTargetPartner())));
                } else if (DEACTIVATE_PARTNER.equalsIgnoreCase(text)) {
                    Partner currentPartner = state.getTargetPartner();
                    currentPartner.setValid(false);
                    partnerRepository.save(currentPartner);
                    yield generateSendMessage(telegramId, "Партнёр успешно деактивирован.",
                        getUserActionKeyboard(userRepository.findById(telegramId),
                            Optional.ofNullable(state.getTargetPartner())));
                } else {
                    yield goToMainMenu(telegramId);
                }
            }

            case VERIFY_NEW_DISCOUNT_PERCENT -> {
                try {
                    short discountPercent = Short.parseShort(text);
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

            case SEND_DISCOUNT_CODE -> {
                yield generateSendMessage(telegramId, """
                        Ваш скидочный код: %06d
                        Оставшееся время действия кода(в секундах): %s
                        Если ваш код, и код у партнера РСМ не совпадают, нажмите "Обновить код".
                        """.formatted(discountCodeService.getDiscountCode(), discountCodeService.getSecondsLeft()),
                    menus.get(UPDATE_DISCOUNT_CODE_MENU));
            }
        };
    }

    private ReplyKeyboard getUserActionKeyboard(Optional<User> userOptional, Optional<Partner> partnerOptional) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        InlineKeyboardButton toMainMenuButton = new InlineKeyboardButton(TO_MAIN_MENU);
        toMainMenuButton.setCallbackData(toMainMenuButton.getText());
        InlineKeyboardButton toPartnersButton = new InlineKeyboardButton(PARTNERS_LIST);
        toPartnersButton.setCallbackData(toPartnersButton.getText());
        keyboard.add(List.of(toMainMenuButton));
        keyboard.add(List.of(toPartnersButton));
        if (userOptional.isPresent() && userOptional.get().getUserRole() != USER) {
            if (partnerOptional.isPresent() && partnerOptional.get().isValid()) {
                InlineKeyboardButton changeDiscountButton = new InlineKeyboardButton(CHANGE_DISCOUNT);
                changeDiscountButton.setCallbackData(changeDiscountButton.getText());
                InlineKeyboardButton deactivatePartner = new InlineKeyboardButton(DEACTIVATE_PARTNER);
                deactivatePartner.setCallbackData(deactivatePartner.getText());
                keyboard.add(List.of(changeDiscountButton, deactivatePartner));

            } else if (partnerOptional.isPresent() && !partnerOptional.get().isValid()) {
                InlineKeyboardButton activatePartner = new InlineKeyboardButton(ACTIVATE_PARTNER);
                activatePartner.setCallbackData(activatePartner.getText());
                keyboard.add(List.of(activatePartner));
            }
            if (userOptional.isPresent()) {
                InlineKeyboardButton getDiscountButton = new InlineKeyboardButton(GET_DISCOUNT_CODE);
                getDiscountButton.setCallbackData(getDiscountButton.getText());
                keyboard.add(List.of(getDiscountButton));
                if (userOptional.get().getUserRole() != USER) {
                    InlineKeyboardButton changeDiscountButton = new InlineKeyboardButton(CHANGE_DISCOUNT);
                    changeDiscountButton.setCallbackData(changeDiscountButton.getText());
                    keyboard.add(List.of(changeDiscountButton));
                }
            }

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