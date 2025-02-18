package net.dunice.mk.rsmtelegrambot.handler.messagehandler;

import lombok.RequiredArgsConstructor;
import net.dunice.mk.rsmtelegrambot.constant.Menu;
import net.dunice.mk.rsmtelegrambot.dto.MessageDto;
import net.dunice.mk.rsmtelegrambot.entity.Category;
import net.dunice.mk.rsmtelegrambot.event.PartnerRegisteredEvent;
import net.dunice.mk.rsmtelegrambot.handler.state.BasicState;
import net.dunice.mk.rsmtelegrambot.handler.state.PartnerEditingState;
import net.dunice.mk.rsmtelegrambot.repository.CategoryRepository;
import net.dunice.mk.rsmtelegrambot.repository.PartnerRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.CANCEL;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.CANCEL_CHANGES;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.CHANGE_ADDRESS;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.CHANGE_CATEGORY;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.CHANGE_INFO;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.CHANGE_LOGO;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.CHANGE_NAME;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.CHANGE_PHONE_NUMBER;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.CONFIRM_CHANGES;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.CANCEL_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.CATEGORY_MENU;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.BasicStep.IN_PARTNER_MENU;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.BasicStep.PARTNER_EDITING;
import static net.dunice.mk.rsmtelegrambot.handler.state.PartnerEditingState.PartnerEditingStep.CHOICE_OPTION;
import static net.dunice.mk.rsmtelegrambot.handler.state.PartnerEditingState.PartnerEditingStep.SHOW_PARTNERS_DETAILS;
import static net.dunice.mk.rsmtelegrambot.handler.state.PartnerEditingState.PartnerEditingStep.UPDATE_ADDRESS;
import static net.dunice.mk.rsmtelegrambot.handler.state.PartnerEditingState.PartnerEditingStep.UPDATE_CATEGORY;
import static net.dunice.mk.rsmtelegrambot.handler.state.PartnerEditingState.PartnerEditingStep.UPDATE_INFO;
import static net.dunice.mk.rsmtelegrambot.handler.state.PartnerEditingState.PartnerEditingStep.UPDATE_LOGO;
import static net.dunice.mk.rsmtelegrambot.handler.state.PartnerEditingState.PartnerEditingStep.UPDATE_NAME;
import static net.dunice.mk.rsmtelegrambot.handler.state.PartnerEditingState.PartnerEditingStep.UPDATE_PHONE_NUMBER;

@Service
@RequiredArgsConstructor
public class PartnerEditingHandler implements MessageHandler{
    private static final ReplyKeyboard PARTNER_UPDATE_OPTIONS = createPartnerUpdateOptions();
    private static final String PARTNER_INFO = """
        Партнер: %s
        Категория: %s
        Информация о партнере: %s
        Номер телефона: %s
        Адрес: %s
        Выберите, что хотите изменить:
        """;

    private final Map<Long, PartnerEditingState> partnerEditingStates;
    private final Map<Menu, ReplyKeyboard> menus;
    private final Map<Long, BasicState> basicStates;
    private final PartnerRepository partnerRepository;
    private final CategoryRepository categoryRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public BasicState.BasicStep getStep() {
        return PARTNER_EDITING;
    }

    @Override
    public PartialBotApiMethod<Message> handle(MessageDto messageDto, Long telegramId) {
        String text = messageDto.getText();
        PartnerEditingState state = partnerEditingStates.get(telegramId);
        if (state == null) {
            partnerEditingStates.put(telegramId, (state = new PartnerEditingState()));
            state.setPartner(partnerRepository.findById(telegramId).get());
        }

        if (StringUtils.equalsAny(text, CANCEL_CHANGES, CONFIRM_CHANGES)) {
            if (text.equals(CONFIRM_CHANGES)) {
                state.getPartner().setValid(false);
                partnerRepository.save(state.getPartner());
                eventPublisher.publishEvent(new PartnerRegisteredEvent(state.getPartner()));
            }
            return goToMainMenu(telegramId);
        }

        if (Objects.equals(text, CANCEL)) {
            state.setStep(SHOW_PARTNERS_DETAILS);
        }

        return switch (state.getStep()) {
            case SHOW_PARTNERS_DETAILS -> handleShowPartnersDetails(state, telegramId);
            case CHOICE_OPTION -> handleChoiceOption(state, telegramId, text);
            case UPDATE_LOGO -> handleUpdateLogo(state, telegramId, messageDto);
            case UPDATE_NAME -> handleUpdateName(state, telegramId, text);
            case UPDATE_CATEGORY -> handleUpdateCategory(state, telegramId, text);
            case UPDATE_INFO -> handleUpdateInfo(state, telegramId, text);
            case UPDATE_PHONE_NUMBER -> handleUpdatePhoneNumber(state, telegramId, text);
            case UPDATE_ADDRESS -> handleUpdateAddress(state, telegramId, text);
        };
    }

    private PartialBotApiMethod<Message> handleShowPartnersDetails(PartnerEditingState state, Long telegramId) {
        String partnerDescription = PARTNER_INFO.formatted(
            state.getPartner().getName(),
            state.getPartner().getCategory().getCategoryName(),
            state.getPartner().getPartnersInfo(),
            state.getPartner().getPhoneNumber(),
            getHyperlinkFromAddress(state.getPartner().getAddress()));

        state.setStep(CHOICE_OPTION);

        byte[] logo = state.getPartner().getLogo();
        if (logo != null && logo.length > 0) {
            SendPhoto sendPhoto = generateImageMessage(telegramId, partnerDescription, PARTNER_UPDATE_OPTIONS, logo);
            sendPhoto.setParseMode("HTML");
            return sendPhoto;
        } else {
            SendMessage sendMessage = generateSendMessage(telegramId, partnerDescription, PARTNER_UPDATE_OPTIONS);
            sendMessage.setParseMode("HTML");
            return sendMessage;
        }
    }

    private PartialBotApiMethod<Message> handleChoiceOption(PartnerEditingState state, Long telegramId, String text) {
        if (text == null) {
            state.setStep(SHOW_PARTNERS_DETAILS);
            return handleShowPartnersDetails(state, telegramId);
        }
        return switch (text) {
            case CHANGE_LOGO -> {
                state.setStep(UPDATE_LOGO);
                yield generateSendMessage(telegramId, "Отправьте ваш логотип (изображение):",
                    menus.get(CANCEL_MENU));
            }
            case CHANGE_NAME -> {
                state.setStep(UPDATE_NAME);
                yield generateSendMessage(telegramId, "Введите название партнёра:", menus.get(CANCEL_MENU));
            }
            case CHANGE_CATEGORY -> {
                state.setStep(UPDATE_CATEGORY);
                yield generateSendMessage(telegramId, "Выберите название категории:", menus.get(CATEGORY_MENU));
            }
            case CHANGE_INFO -> {
                state.setStep(UPDATE_INFO);
                yield generateSendMessage(telegramId, "Введите информацию о партнёре:", menus.get(CANCEL_MENU));
            }
            case CHANGE_PHONE_NUMBER -> {
                state.setStep(UPDATE_PHONE_NUMBER);
                yield generateSendMessage(telegramId, "Введите номер телефона:", menus.get(CANCEL_MENU));
            }
            case CHANGE_ADDRESS -> {
                state.setStep(UPDATE_ADDRESS);
                yield generateSendMessage(telegramId, "Введите адрес партнёра:", menus.get(CANCEL_MENU));
            }
            default -> generateSendMessage(telegramId, "Неизвестная команда: " + text, menus.get(CANCEL_MENU));
        };
    }

    private PartialBotApiMethod<Message> handleUpdateLogo(PartnerEditingState state, Long telegramId, MessageDto messageDto) {
        if (messageDto.getImage() != null) {
            state.getPartner().setLogo(messageDto.getImage());
            state.setStep(CHOICE_OPTION);
            return handleShowPartnersDetails(state, telegramId);
        } else {
            return generateSendMessage(telegramId, "Логотип должен быть изображением. Повторите ввод:",
                menus.get(CANCEL_MENU));
        }
    }

    private PartialBotApiMethod<Message> handleUpdateName(PartnerEditingState state, Long telegramId, String text) {
        if (text != null && text.length() <= 100) {
            state.getPartner().setName(text.strip());
            state.setStep(CHOICE_OPTION);
            return handleShowPartnersDetails(state, telegramId);
        } else {
            return generateSendMessage(telegramId, "Название организации не должно быть пустым. Повторите ввод:",
                menus.get(CANCEL_MENU));
        }
    }

    private PartialBotApiMethod<Message> handleUpdateCategory(PartnerEditingState state, Long telegramId, String text) {
        Optional<Category> category = categoryRepository.findByCategoryName(text);
        if (category.isPresent()) {
            state.getPartner().setCategory(category.get());
            state.setStep(CHOICE_OPTION);
            return handleShowPartnersDetails(state, telegramId);
        } else {
            return generateSendMessage(telegramId, "Категория не найдена. Повторите ввод:",
                menus.get(CATEGORY_MENU));
        }
    }

    private PartialBotApiMethod<Message> handleUpdateInfo(PartnerEditingState state, Long telegramId, String text) {
        if (text != null && text.length() <= 250) {
            state.getPartner().setPartnersInfo(text.strip());
            state.setStep(CHOICE_OPTION);
            return handleShowPartnersDetails(state, telegramId);
        } else {
            return generateSendMessage(telegramId, "Некорректная информация. Повторите ввод:",
                menus.get(CANCEL_MENU));
        }
    }

    private PartialBotApiMethod<Message> handleUpdatePhoneNumber(PartnerEditingState state, Long telegramId, String text) {
        if (text != null &&
            text.matches("\\+7[\\s-]?\\(?\\d{3}\\)?[\\s-]?\\d{3}[\\s-]?\\d{2}[\\s-]?\\d{2}")) {
            state.getPartner().setPhoneNumber(text.strip());
            state.setStep(CHOICE_OPTION);
            return handleShowPartnersDetails(state, telegramId);
        } else {
            return generateSendMessage(telegramId,
                "Номер телефона должен соответствовать формату +7 для России. Повторите ввод:",
                menus.get(CANCEL_MENU));
        }
    }

    private PartialBotApiMethod<Message> handleUpdateAddress(PartnerEditingState state, Long telegramId, String text) {
        if (text != null && text.length() <= 255) {
            String address = text.trim();
            String[] parts = address.split(" ");

            if (parts.length != 2) {
                return generateSendMessage(telegramId,
                    "Адрес должен соответствовать формату (улица и номер дома, например Крестьянская 207), Повторите ввод (до 255 символов):",
                    menus.get(CANCEL_MENU));
            }
            if (!parts[1].matches("^[0-9]+[A-Za-zА-Яа-я]?$|^[0-9]+/[0-9]+$|^[0-9]+[A-Za-zА-Яа-я]?/[0-9]+$")) {
                return generateSendMessage(telegramId,
                    "Номер дома должен быть в формате '123', '123А' или '123/456'. Повторите ввод:",
                    menus.get(CANCEL_MENU));
            }

            address = "г. Майкоп, ул. %s, д. %s".formatted(parts[0], parts[1]);
            state.getPartner().setAddress(address);
            state.setStep(CHOICE_OPTION);
            return handleShowPartnersDetails(state, telegramId);
        } else {
            return generateSendMessage(telegramId,
                "Адрес слишком длинный. Повторите ввод (до 255 символов):", menus.get(CANCEL_MENU));
        }
    }

    private static ReplyKeyboard createPartnerUpdateOptions() {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        InlineKeyboardButton changeLogoButton = new InlineKeyboardButton(CHANGE_LOGO);
        changeLogoButton.setCallbackData(changeLogoButton.getText());
        InlineKeyboardButton changeNameButton = new InlineKeyboardButton(CHANGE_NAME);
        changeNameButton.setCallbackData(changeNameButton.getText());
        keyboard.add(List.of(changeLogoButton, changeNameButton));

        InlineKeyboardButton changeCategoryButton = new InlineKeyboardButton(CHANGE_CATEGORY);
        changeCategoryButton.setCallbackData(changeCategoryButton.getText());
        InlineKeyboardButton changeInfoButton = new InlineKeyboardButton(CHANGE_INFO);
        changeInfoButton.setCallbackData(changeInfoButton.getText());
        keyboard.add(List.of(changeCategoryButton, changeInfoButton));

        InlineKeyboardButton changePhoneNumberButton = new InlineKeyboardButton(CHANGE_PHONE_NUMBER);
        changePhoneNumberButton.setCallbackData(changePhoneNumberButton.getText());
        InlineKeyboardButton changeAddressButton = new InlineKeyboardButton(CHANGE_ADDRESS);
        changeAddressButton.setCallbackData(changeAddressButton.getText());
        keyboard.add(List.of(changePhoneNumberButton, changeAddressButton));

        InlineKeyboardButton mainMenuButton = new InlineKeyboardButton(CANCEL_CHANGES);
        mainMenuButton.setCallbackData(mainMenuButton.getText());
        InlineKeyboardButton confirmButton = new InlineKeyboardButton(CONFIRM_CHANGES);
        confirmButton.setCallbackData(confirmButton.getText());
        keyboard.add(List.of(mainMenuButton, confirmButton));

        inlineKeyboardMarkup.setKeyboard(keyboard);
        return inlineKeyboardMarkup;
    }

    private SendMessage goToMainMenu(Long telegramId) {
        basicStates.get(telegramId).setStep(IN_PARTNER_MENU);
        partnerEditingStates.remove(telegramId);
        return generateSendMessage(telegramId, "Выберите раздел:", menus.get(Menu.PARTNER_MAIN_MENU));
    }

    private String getHyperlinkFromAddress(String address) {
        String encodedAddress = URLEncoder.encode(address, StandardCharsets.UTF_8);
        String url = "https://yandex.ru/maps/?text=" + encodedAddress;
        return String.format("<a href=\"%s\">%s</a>", url, address);
    }
}
