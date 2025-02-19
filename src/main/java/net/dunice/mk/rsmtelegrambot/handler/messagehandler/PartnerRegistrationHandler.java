package net.dunice.mk.rsmtelegrambot.handler.messagehandler;

import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.CANCEL;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.TO_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.CANCEL_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.CATEGORY_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.GO_TO_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.BasicStep;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.BasicStep.IN_PARTNER_MENU;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.BasicStep.PARTNER_REGISTRATION;
import static net.dunice.mk.rsmtelegrambot.handler.state.PartnerRegistrationState.PartnerRegistrationStep.ADDRESS;
import static net.dunice.mk.rsmtelegrambot.handler.state.PartnerRegistrationState.PartnerRegistrationStep.CATEGORY;
import static net.dunice.mk.rsmtelegrambot.handler.state.PartnerRegistrationState.PartnerRegistrationStep.DISCOUNT_DATE;
import static net.dunice.mk.rsmtelegrambot.handler.state.PartnerRegistrationState.PartnerRegistrationStep.DISCOUNT_PERCENT;
import static net.dunice.mk.rsmtelegrambot.handler.state.PartnerRegistrationState.PartnerRegistrationStep.FINISH;
import static net.dunice.mk.rsmtelegrambot.handler.state.PartnerRegistrationState.PartnerRegistrationStep.LOGO;
import static net.dunice.mk.rsmtelegrambot.handler.state.PartnerRegistrationState.PartnerRegistrationStep.PARTNER_INFO;
import static net.dunice.mk.rsmtelegrambot.handler.state.PartnerRegistrationState.PartnerRegistrationStep.VALIDATE_PARTNER_NAME;

import lombok.RequiredArgsConstructor;
import net.dunice.mk.rsmtelegrambot.constant.Menu;
import net.dunice.mk.rsmtelegrambot.dto.MessageDto;
import net.dunice.mk.rsmtelegrambot.entity.Category;
import net.dunice.mk.rsmtelegrambot.entity.Partner;
import net.dunice.mk.rsmtelegrambot.event.PartnerRegisteredEvent;
import net.dunice.mk.rsmtelegrambot.handler.state.BasicState;
import net.dunice.mk.rsmtelegrambot.handler.state.PartnerRegistrationState;
import net.dunice.mk.rsmtelegrambot.repository.CategoryRepository;
import net.dunice.mk.rsmtelegrambot.repository.PartnerRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PartnerRegistrationHandler implements MessageHandler {

    private final Map<Long, PartnerRegistrationState> partnerRegistrationStates;
    private final Map<Long, BasicState> basicStates;
    private final EnumMap<Menu, ReplyKeyboard> menus;
    private final ApplicationEventPublisher eventPublisher;
    private final PartnerRepository partnerRepository;
    private final CategoryRepository categoryRepository;

    @Override
    public BasicStep getStep() {
        return PARTNER_REGISTRATION;
    }

    @Override
    public SendMessage handle(MessageDto messageDto, Long telegramId) {
        String text = messageDto.getText();

        PartnerRegistrationState state = partnerRegistrationStates.get(telegramId);
        if (state == null) {
            state = new PartnerRegistrationState();
            partnerRegistrationStates.put(telegramId, state);
        }

        if (CANCEL.equals(text)) {
            basicStates.remove(telegramId);
            partnerRegistrationStates.remove(telegramId);
            return generateSendMessage(
                telegramId,
                "Регистрация отменена, введите /start, если хотите попробовать заново:"
            );
        }

        return switch (state.getStep()) {
            case REQUEST_PARTNER_NAME    -> handleRequestPartnerName(telegramId, state);
            case VALIDATE_PARTNER_NAME   -> handleValidatePartnerName(text, telegramId, state);
            case PHONE_NUMBER            -> handlePhoneNumber(text, telegramId, state);
            case DISCOUNT_PERCENT        -> handleDiscountPercent(text, telegramId, state);
            case CATEGORY                -> handleCategory(text, telegramId, state);
            case LOGO                    -> handleLogo(messageDto, telegramId, state);
            case DISCOUNT_DATE           -> handleDiscountDate(text, telegramId, state);
            case PARTNER_INFO            -> handlePartnerInfo(text, telegramId, state);
            case ADDRESS                 -> handleAddress(text, telegramId, state);
            case FINISH                  -> handleFinish(text, telegramId);
        };
    }

    private SendMessage handleRequestPartnerName(Long telegramId, PartnerRegistrationState state) {
        state.setStep(VALIDATE_PARTNER_NAME);
        return generateSendMessage(
            telegramId,
            "Введите название партнёра:",
            menus.get(CANCEL_MENU)
        );
    }

    private SendMessage handleValidatePartnerName(String text,
                                                  Long telegramId,
                                                  PartnerRegistrationState state) {
        if (text == null || text.length() > 100) {
            return generateSendMessage(
                telegramId,
                "Название организации не должно быть больше 100 символов или пустым. Повторите ввод:",
                menus.get(CANCEL_MENU)
            );
        }
        state.setName(text.strip());
        state.setStep(PartnerRegistrationState.PartnerRegistrationStep.PHONE_NUMBER);

        return generateSendMessage(
            telegramId,
            "Введите номер телефона:",
            menus.get(CANCEL_MENU)
        );
    }

    private SendMessage handlePhoneNumber(String text,
                                          Long telegramId,
                                          PartnerRegistrationState state) {
        if (text != null &&
            text.matches("\\+7[\\s-]?\\(?\\d{3}\\)?[\\s-]?\\d{3}[\\s-]?\\d{2}[\\s-]?\\d{2}")) {
            state.setPhoneNumber(text.strip());
            state.setStep(DISCOUNT_PERCENT);
            return generateSendMessage(
                telegramId,
                "Введите процент скидки:",
                menus.get(CANCEL_MENU)
            );
        } else {
            return generateSendMessage(
                telegramId,
                "Номер телефона должен соответствовать формату +7 для России. Повторите ввод:",
                menus.get(CANCEL_MENU)
            );
        }
    }

    private SendMessage handleDiscountPercent(String text,
                                              Long telegramId,
                                              PartnerRegistrationState state) {
        if (text == null) {
            return generateSendMessage(
                telegramId,
                "Процент скидки должен быть указан. Повторите ввод:",
                menus.get(CANCEL_MENU)
            );
        }
        try {
            Short discountPercent = Short.parseShort(text.strip());
            if (discountPercent < 0 || discountPercent >= 100) {
                return generateSendMessage(
                    telegramId,
                    "Процент скидки должен быть от 0 до 100. Повторите ввод:",
                    menus.get(CANCEL_MENU)
                );
            }
            state.setDiscountPercent(discountPercent);
            state.setStep(CATEGORY);
            return generateSendMessage(
                telegramId,
                "Выберите название категории:",
                menus.get(CATEGORY_MENU)
            );

        } catch (NumberFormatException e) {
            return generateSendMessage(
                telegramId,
                "Процент скидки должен быть числом. Повторите ввод:",
                menus.get(CANCEL_MENU)
            );
        }
    }

    private SendMessage handleCategory(String text,
                                       Long telegramId,
                                       PartnerRegistrationState state) {
        Optional<Category> categoryOpt = categoryRepository.findByCategoryName(text);
        if (categoryOpt.isEmpty()) {
            return generateSendMessage(
                telegramId,
                "Категория не найдена. Повторите ввод:",
                menus.get(CATEGORY_MENU)
            );
        }
        state.setCategory(categoryOpt.get());
        state.setStep(LOGO);
        return generateSendMessage(
            telegramId,
            "Отправьте ваш логотип (изображение):",
            menus.get(CANCEL_MENU)
        );
    }

    private SendMessage handleLogo(MessageDto messageDto,
                                   Long telegramId,
                                   PartnerRegistrationState state) {
        if (messageDto.getImage() != null) {
            state.setLogo(messageDto.getImage());
            state.setStep(DISCOUNT_DATE);
            return generateSendMessage(
                telegramId,
                "Пожалуйста, введите дату и время конца действия скидки в формате (ДД.ММ.ГГГГ-ЧЧ:ММ) :",
                menus.get(CANCEL_MENU)
            );
        } else {
            return generateSendMessage(
                telegramId,
                "Логотип должен быть изображением. Повторите ввод:",
                menus.get(CANCEL_MENU)
            );
        }
    }

    private SendMessage handleDiscountDate(String text,
                                           Long telegramId,
                                           PartnerRegistrationState state) {
        try {
            state.setDiscountDate(LocalDateTime.parse(
                text.trim(),
                DateTimeFormatter.ofPattern("dd.MM.yyyy-HH:mm"))
            );
            state.setStep(PARTNER_INFO);
            return generateSendMessage(
                telegramId,
                "Введите дополнительную информацию о партнёре:",
                menus.get(CANCEL_MENU)
            );
        } catch (DateTimeParseException | NullPointerException e) {
            return generateSendMessage(
                telegramId,
                "Дата должна быть в формате (ДД.ММ.ГГГГ-ЧЧ:ММ). Повторите ввод:",
                menus.get(CANCEL_MENU)
            );
        }
    }

    private SendMessage handlePartnerInfo(String text,
                                          Long telegramId,
                                          PartnerRegistrationState state) {
        if (text == null || text.length() > 250) {
            return generateSendMessage(
                telegramId,
                "Информация слишком длинная. Повторите ввод (до 250 символов):",
                menus.get(CANCEL_MENU)
            );
        }
        state.setInfo(text.trim());
        state.setStep(ADDRESS);
        return generateSendMessage(
            telegramId,
            "Введите адрес (улица и номер дома, например Крестьянская 207):",
            menus.get(CANCEL_MENU)
        );
    }

    private SendMessage handleAddress(String text,
                                      Long telegramId,
                                      PartnerRegistrationState state) {
        if (text == null || text.length() > 255) {
            return generateSendMessage(
                telegramId,
                "Адрес слишком длинный. Повторите ввод (до 255 символов):",
                menus.get(CANCEL_MENU)
            );
        }

        String[] parts = text.trim().split(" ");
        if (parts.length != 2) {
            return generateSendMessage(
                telegramId,
                "Адрес должен соответствовать формату (улица и номер дома, например Крестьянская 207). Повторите ввод:",
                menus.get(CANCEL_MENU)
            );
        }
        if (!parts[1].matches("^[0-9]+[A-Za-zА-Яа-я]?$|^[0-9]+/[0-9]+$|^[0-9]+[A-Za-zА-Яа-я]?/[0-9]+$")) {
            return generateSendMessage(
                telegramId,
                "Номер дома должен быть в формате '123', '123А' или '123/456'. Повторите ввод:",
                menus.get(CANCEL_MENU)
            );
        }

        String address = "г. Майкоп, ул. %s, д. %s".formatted(parts[0], parts[1]);
        state.setAddress(address);

        savePartner(state, telegramId);

        state.setStep(FINISH);
        return generateSendMessage(
            telegramId,
            "Данные добавлены и отправлены на проверку.",
            menus.get(GO_TO_MAIN_MENU)
        );
    }

    private SendMessage handleFinish(String text, Long telegramId) {
        if (TO_MAIN_MENU.equalsIgnoreCase(text)) {
            return goToMainMenu(telegramId);
        }
        return generateSendMessage(telegramId, "Неверная команда.", menus.get(GO_TO_MAIN_MENU));
    }

    private void savePartner(PartnerRegistrationState state, long telegramId) {
        Partner partner = new Partner();
        partner.setPartnerTelegramId(telegramId);
        partner.setName(state.getName());
        partner.setPhoneNumber(state.getPhoneNumber());
        partner.setDiscountPercent(state.getDiscountPercent());
        partner.setCategory(state.getCategory());
        partner.setLogo(state.getLogo());
        partner.setDiscountDate(state.getDiscountDate());
        partner.setPartnersInfo(state.getInfo());
        partner.setAddress(state.getAddress());
        partner.setValid(false);

        partnerRepository.save(partner);
        eventPublisher.publishEvent(new PartnerRegisteredEvent(partner));
    }

    private SendMessage goToMainMenu(Long telegramId) {
        partnerRegistrationStates.remove(telegramId);
        basicStates.get(telegramId).setStep(IN_PARTNER_MENU);
        return generateSendMessage(
            telegramId,
            "Регистрация завершена, профиль партнера станет доступен после проверки администратором"
        );
    }
}


