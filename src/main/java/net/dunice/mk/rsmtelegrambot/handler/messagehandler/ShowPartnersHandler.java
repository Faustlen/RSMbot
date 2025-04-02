package net.dunice.mk.rsmtelegrambot.handler.messagehandler;

import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.ACTIVATE_PARTNER;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.ADD_STOCK;
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
import net.dunice.mk.rsmtelegrambot.entity.Role;
import net.dunice.mk.rsmtelegrambot.entity.User;
import net.dunice.mk.rsmtelegrambot.event.BroadcastPartnersEvent;
import net.dunice.mk.rsmtelegrambot.handler.MenuGenerator;
import net.dunice.mk.rsmtelegrambot.handler.state.BasicState;
import net.dunice.mk.rsmtelegrambot.handler.state.ShowPartnersState;
import net.dunice.mk.rsmtelegrambot.repository.PartnerRepository;
import net.dunice.mk.rsmtelegrambot.repository.UserRepository;
import net.dunice.mk.rsmtelegrambot.service.DiscountCodeService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

    private static final String PARTNER_INFO_FOR_PARTNERS = """
        Партнер: %s
        Категория: %s
        Информация о партнере: %s
        Номер телефона: %s
        Адрес: %s
        """;

    private static final String PARTNER_INFO_FOR_USERS = PARTNER_INFO_FOR_PARTNERS + """
        Процент скидки: %s%%
        Дата окончания скидки: %s
        """;

    private static final String PARTNER_INFO_FOR_ADMIN = PARTNER_INFO_FOR_USERS + "Активирован: %s";

    private static final String CHANGE_DISCOUNT_INFO = """
        Хотите изменить данные у партнера на следующие?
        Партнер: %s
        Процент скидки: %s%%
        Дата окончания скидки: %s
        """;

    private static final String CHANGE_DISCOUNT_NOTIFICATION = """
        Ваши данные были изменены администратором:
        Процент скидки: %s%%
        Дата окончания скидки %s
        """;

    private static final String DISCOUNT_CODE_MESSAGE = """
        Ваш скидочный код: %06d
        Оставшееся время действия кода(в секундах): %s
        Если ваш код, и код у партнера РСМ не совпадают, нажмите "Обновить код".
        """;

    private final ApplicationEventPublisher eventPublisher;
    private final PartnerRepository partnerRepository;
    private final MenuGenerator menuGenerator;
    private final MenuConfig menuConfig;
    private final Map<Menu, ReplyKeyboard> menus;
    private final Map<Long, BasicState> basicStates;
    private final UserRepository userRepository;
    private final Map<Long, ShowPartnersState> showPartnersStates;
    private final DiscountCodeService discountCodeService;
    private final CreateStockHandler createStockHandler;

    @Override
    public BasicStep getStep() {
        return SHOW_PARTNERS;
    }

    @Override
    public PartialBotApiMethod<Message> handle(MessageDto messageDto, Long telegramId) {
        String text = messageDto.getText();

        ShowPartnersState state = showPartnersStates.get(telegramId);
        if (state == null) {
            state = new ShowPartnersState();
            showPartnersStates.put(telegramId, state);
        }

        if (StringUtils.equalsAny(text, TO_MAIN_MENU, CANCEL)) {
            return goToMainMenu(telegramId);
        }

        return switch (state.getStep()) {
            case SHOW_PARTNERS_LIST -> handleShowPartnersList(telegramId, state);
            case SHOW_PARTNER_DETAILS -> handleShowPartnerDetails(messageDto, telegramId, state);
            case HANDLE_USER_ACTION -> handleUserAction(messageDto, telegramId, state);
            case VERIFY_NEW_DISCOUNT_PERCENT -> handleVerifyNewDiscountPercent(messageDto, telegramId, state);
            case VERIFY_NEW_DISCOUNT_DATE -> handleVerifyNewDiscountDate(messageDto, telegramId, state);
            case CONFIRM_CHANGE -> handleConfirmChange(messageDto, telegramId, state);
            case SEND_DISCOUNT_CODE -> handleSendDiscountCode(telegramId);
        };
    }

    public PartialBotApiMethod<Message> handleDetails(MessageDto messageDto, Long telegramId) {
        ShowPartnersState state = showPartnersStates.get(telegramId);
        if (state == null) {
            state = new ShowPartnersState();
            showPartnersStates.put(telegramId, state);
        }

        state.setStep(SHOW_PARTNER_DETAILS);
        return handleShowPartnerDetails(messageDto, telegramId, state);
    }

    private PartialBotApiMethod<Message> handleShowPartnersList(Long telegramId, ShowPartnersState state) {
        List<Partner> partners = partnerRepository.findAll();
        Optional<User> userOptional = userRepository.findById(telegramId);
        state.setStep(SHOW_PARTNER_DETAILS);

        if (userOptional.isEmpty() || userOptional.get().getUserRole() == USER) {
            partners = partners.stream()
                .filter(partner -> partner.isValid()
                    && partner.getDiscountDate().isAfter(LocalDateTime.now()))
                .toList();
            return generateSendMessage(telegramId,
                "Партнеры РСМ:",
                menuConfig.getPartnersListKeyboard(partners, true));
        } else {
            return generateSendMessage(telegramId,
                "Партнеры РСМ:",
                menuConfig.getPartnersListKeyboard(partners, false));
        }
    }

    private PartialBotApiMethod<Message> handleShowPartnerDetails(MessageDto messageDto,
                                                                  Long telegramId,
                                                                  ShowPartnersState state) {
        String text = messageDto.getText();

        if (text == null) {
            state.setStep(SHOW_PARTNERS_LIST);
            return handleShowPartnersList(telegramId, state);
        }

        if (text.endsWith("✅") || text.endsWith("❌")) {
            text = text.substring(0, text.length() - 2).trim();
        }

        Optional<Partner> partnerOptional = partnerRepository.findByName(text);
        if (partnerOptional.isEmpty()) {
            return generateSendMessage(telegramId, "Нет партнера с таким названием.");
        }

        Partner targetPartner = partnerOptional.get();
        Optional<User> userOptional = userRepository.findById(telegramId);
        String description = getPartnerDescription(targetPartner, userOptional);

        state.setTargetPartner(targetPartner);
        state.setStep(HANDLE_USER_ACTION);

        byte[] logo = targetPartner.getLogo();
        if (isLogoPresent(logo)) {
            SendPhoto sendPhoto = generateImageMessage(
                telegramId,
                description,
                getUserActionKeyboard(userOptional, partnerOptional),
                logo
            );
            sendPhoto.setParseMode("HTML");
            return sendPhoto;
        } else {
            SendMessage sendMessage = generateSendMessage(
                telegramId,
                description,
                getUserActionKeyboard(userOptional, partnerOptional)
            );
            sendMessage.setParseMode("HTML");
            return sendMessage;
        }
    }

    private PartialBotApiMethod<Message> handleUserAction(MessageDto messageDto,
                                                          Long telegramId,
                                                          ShowPartnersState state) {
        String text = messageDto.getText();

        if (PARTNERS_LIST.equalsIgnoreCase(text)) {
            state.setStep(SHOW_PARTNERS_LIST);
            return handleShowPartnersList(telegramId, state);

        } else if (CHANGE_DISCOUNT.equalsIgnoreCase(text) &&
            basicStates.get(telegramId).getUser().getUserRole() != USER) {
            state.setStep(VERIFY_NEW_DISCOUNT_PERCENT);
            return generateSendMessage(
                telegramId,
                "Пожалуйста, введите новый процент скидки (от 0 до 100):",
                menus.get(CANCEL_MENU)
            );

        } else if (GET_DISCOUNT_CODE.equalsIgnoreCase(text)) {
            state.setStep(SEND_DISCOUNT_CODE);
            return handleSendDiscountCode(telegramId);

        } else if (ACTIVATE_PARTNER.equalsIgnoreCase(text)) {
            Partner currentPartner = state.getTargetPartner();
            currentPartner.setValid(true);
            partnerRepository.save(currentPartner);

            return generateSendMessage(
                telegramId,
                "Партнёр успешно активирован.",
                getUserActionKeyboard(userRepository.findById(telegramId),
                    Optional.of(currentPartner))
            );

        } else if (DEACTIVATE_PARTNER.equalsIgnoreCase(text)) {
            Partner currentPartner = state.getTargetPartner();
            currentPartner.setValid(false);
            partnerRepository.save(currentPartner);

            return generateSendMessage(
                telegramId,
                "Партнёр успешно деактивирован.",
                getUserActionKeyboard(userRepository.findById(telegramId),
                    Optional.of(currentPartner))
            );
        } else if (ADD_STOCK.equalsIgnoreCase(text)) {
            Partner currentPartner = state.getTargetPartner();
            messageDto.setText(currentPartner.getPartnerTelegramId().toString());
            return createStockHandler.handle(messageDto, telegramId);
        }
        return goToMainMenu(telegramId);
    }

    private PartialBotApiMethod<Message> handleVerifyNewDiscountPercent(MessageDto messageDto,
                                                                        Long telegramId,
                                                                        ShowPartnersState state) {
        String text = messageDto.getText();
        try {
            short discountPercent = Short.parseShort(text);
            if (discountPercent < 0 || discountPercent >= 100) {
                return generateSendMessage(
                    telegramId,
                    "Процент скидки должен быть от 0 до 100. Повторите ввод:",
                    menus.get(CANCEL_MENU)
                );
            }
            state.setDiscountPercent(discountPercent);
            state.setStep(VERIFY_NEW_DISCOUNT_DATE);

            return generateSendMessage(
                telegramId,
                "Пожалуйста, введите дату конца действия скидки в формате (ДД.ММ.ГГГГ-ЧЧ:ММ)",
                menus.get(CANCEL_MENU)
            );

        } catch (NumberFormatException e) {
            return generateSendMessage(
                telegramId,
                "Процент скидки должен быть числом. Повторите ввод:",
                menus.get(CANCEL_MENU)
            );
        }
    }

    private PartialBotApiMethod<Message> handleVerifyNewDiscountDate(MessageDto messageDto,
                                                                     Long telegramId,
                                                                     ShowPartnersState state) {
        String text = messageDto.getText();

        if (text == null) {
            return generateSendMessage(
                telegramId,
                "Пожалуйста, введите дату конца действия скидки в формате (ДД.ММ.ГГГГ-ЧЧ:ММ)",
                menus.get(CANCEL_MENU)
            );
        }

        try {
            LocalDateTime discountDate = LocalDateTime.parse(
                text.trim(),
                DateTimeFormatter.ofPattern("dd.MM.yyyy-HH:mm")
            );
            state.setDiscountDate(discountDate);
            state.setStep(CONFIRM_CHANGE);

            String msg = CHANGE_DISCOUNT_INFO.formatted(
                state.getTargetPartner().getName(),
                state.getDiscountPercent(),
                discountDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy | HH:mm"))
            );
            return generateSendMessage(telegramId, msg, menus.get(SELECTION_MENU));

        } catch (DateTimeParseException | NullPointerException e) {
            return generateSendMessage(
                telegramId,
                "Дата должна быть в формате (ДД.ММ.ГГГГ-ЧЧ:ММ). Повторите ввод:",
                menus.get(CANCEL_MENU)
            );
        }
    }

    private PartialBotApiMethod<Message> handleConfirmChange(MessageDto messageDto,
                                                             Long telegramId,
                                                             ShowPartnersState state) {
        String text = messageDto.getText();
        String responseMessage;

        if ("Да".equalsIgnoreCase(text)) {
            Partner currentPartner = state.getTargetPartner();
            currentPartner.setDiscountPercent(state.getDiscountPercent());
            currentPartner.setDiscountDate(state.getDiscountDate());
            partnerRepository.save(currentPartner);

            responseMessage = "Данные партнера успешно изменены.";
            eventPublisher.publishEvent(
                new BroadcastPartnersEvent(
                    CHANGE_DISCOUNT_NOTIFICATION.formatted(
                        currentPartner.getDiscountPercent(),
                        currentPartner.getDiscountDate() == null
                            ? "Неограниченно"
                            : currentPartner.getDiscountDate().toLocalDate()
                    ),
                    List.of(currentPartner)
                )
            );
        }
        else if ("Нет".equalsIgnoreCase(text)) {
            responseMessage = "Изменение данных партнера отменено.";
        } else {
            String msg = CHANGE_DISCOUNT_INFO.formatted(
                state.getTargetPartner().getName(),
                state.getDiscountPercent(),
                state.getDiscountDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy | HH:mm"))
            );
            return generateSendMessage(telegramId, msg, menus.get(SELECTION_MENU));
        }

        return generateSendMessage(telegramId, responseMessage, menus.get(GO_TO_MAIN_MENU));
    }

    private PartialBotApiMethod<Message> handleSendDiscountCode(Long telegramId) {
        return generateSendMessage(
            telegramId,
            DISCOUNT_CODE_MESSAGE.formatted(
                discountCodeService.getDiscountCode(),
                discountCodeService.getSecondsLeft()
            ),
            menus.get(UPDATE_DISCOUNT_CODE_MENU)
        );
    }

    private SendMessage goToMainMenu(Long telegramId) {
        showPartnersStates.remove(telegramId);

        if (isTgUserPartner(telegramId)) {
            basicStates.get(telegramId).setStep(IN_PARTNER_MENU);
            return generateSendMessage(
                telegramId,
                "Выберите раздел:",
                menus.get(Menu.PARTNER_MAIN_MENU)
            );
        } else {
            basicStates.get(telegramId).setStep(IN_MAIN_MENU);
            return menuGenerator.generateRoleSpecificMainMenu(
                telegramId,
                userRepository.findByTelegramId(telegramId).get().getUserRole()
            );
        }
    }

    private String getPartnerDescription(Partner partner, Optional<User> userOptional) {
        if (userOptional.isEmpty()) {
            return partner.isValid()
                ? PARTNER_INFO_FOR_PARTNERS.formatted(
                partner.getName(),
                partner.getCategory().getCategoryName(),
                partner.getPartnersInfo(),
                partner.getPhoneNumber(),
                getHyperlinkFromAddress(partner.getAddress())
            )
                : "Информация о партнёре (%s) недоступна, пока не пройдет проверку администратором."
                .formatted(partner.getName());
        }

        Role role = userOptional.get().getUserRole();
        if (role == USER) {
            return partner.isValid()
                ? PARTNER_INFO_FOR_USERS.formatted(
                partner.getName(),
                partner.getCategory().getCategoryName(),
                partner.getPartnersInfo(),
                partner.getPhoneNumber(),
                getHyperlinkFromAddress(partner.getAddress()),
                partner.getDiscountPercent(),
                partner.getDiscountDate() == null
                    ? "Неограниченно"
                    : partner.getDiscountDate().toLocalDate()
            )
                : "Информация о партнёре (%s) недоступна, пока не пройдет проверку администратором."
                .formatted(partner.getName());
        }
        return PARTNER_INFO_FOR_ADMIN.formatted(
            partner.getName(),
            partner.getCategory().getCategoryName(),
            partner.getPartnersInfo(),
            partner.getPhoneNumber(),
            getHyperlinkFromAddress(partner.getAddress()),
            partner.getDiscountPercent(),
            partner.getDiscountDate() == null
                ? "Неограниченно"
                : partner.getDiscountDate().toLocalDate(),
            partner.isValid() ? "Да" : "Нет"
        );
    }

    private ReplyKeyboard getUserActionKeyboard(Optional<User> userOptional, Optional<Partner> partnerOptional) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        InlineKeyboardButton toMainMenuButton = new InlineKeyboardButton(TO_MAIN_MENU);
        toMainMenuButton.setCallbackData(TO_MAIN_MENU);

        InlineKeyboardButton toPartnersButton = new InlineKeyboardButton(PARTNERS_LIST);
        toPartnersButton.setCallbackData(PARTNERS_LIST);

        keyboard.add(List.of(toMainMenuButton));
        keyboard.add(List.of(toPartnersButton));

        if (userOptional.isPresent() && userOptional.get().getUserRole() != USER) {
            if (partnerOptional.isPresent()) {
                Partner partner = partnerOptional.get();
                if (partner.isValid()) {
                    InlineKeyboardButton createStockButton = new InlineKeyboardButton(ADD_STOCK);
                    createStockButton.setCallbackData(ADD_STOCK);
                    keyboard.add(List.of(createStockButton));

                    InlineKeyboardButton changeDiscountButton = new InlineKeyboardButton(CHANGE_DISCOUNT);
                    changeDiscountButton.setCallbackData(CHANGE_DISCOUNT);

                    InlineKeyboardButton deactivateButton = new InlineKeyboardButton(DEACTIVATE_PARTNER);
                    deactivateButton.setCallbackData(DEACTIVATE_PARTNER);

                    keyboard.add(List.of(changeDiscountButton, deactivateButton));
                } else {
                    InlineKeyboardButton activatePartner = new InlineKeyboardButton(ACTIVATE_PARTNER);
                    activatePartner.setCallbackData(ACTIVATE_PARTNER);

                    keyboard.add(List.of(activatePartner));
                }
            }
        }

        InlineKeyboardButton getDiscountButton = new InlineKeyboardButton(GET_DISCOUNT_CODE);
        getDiscountButton.setCallbackData(GET_DISCOUNT_CODE);
        keyboard.add(List.of(getDiscountButton));

        inlineKeyboardMarkup.setKeyboard(keyboard);
        return inlineKeyboardMarkup;
    }

    private String getHyperlinkFromAddress(String address) {
        String encodedAddress = URLEncoder.encode(address, StandardCharsets.UTF_8);
        String url = "https://yandex.ru/maps/?text=" + encodedAddress;
        return String.format("<a href=\"%s\">%s</a>", url, address);
    }

    private boolean isLogoPresent(byte[] logo) {
        return logo != null && logo.length > 0;
    }

    private boolean isTgUserPartner(Long telegramId) {
        return partnerRepository.existsById(telegramId);
    }
}
