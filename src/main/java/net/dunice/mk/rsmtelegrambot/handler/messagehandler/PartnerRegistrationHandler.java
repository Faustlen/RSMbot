package net.dunice.mk.rsmtelegrambot.handler.messagehandler;

import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.TO_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.GO_TO_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.PARTNER_REGISTRATION;
import static net.dunice.mk.rsmtelegrambot.handler.state.step.PartnerRegistrationStep.CATEGORY;
import static net.dunice.mk.rsmtelegrambot.handler.state.step.PartnerRegistrationStep.DISCOUNT_DATE;
import static net.dunice.mk.rsmtelegrambot.handler.state.step.PartnerRegistrationStep.DISCOUNT_PERCENT;
import static net.dunice.mk.rsmtelegrambot.handler.state.step.PartnerRegistrationStep.FINISH;
import static net.dunice.mk.rsmtelegrambot.handler.state.step.PartnerRegistrationStep.LOGO;
import static net.dunice.mk.rsmtelegrambot.handler.state.step.PartnerRegistrationStep.PARTNER_INFO;
import static net.dunice.mk.rsmtelegrambot.handler.state.step.PartnerRegistrationStep.PHONE_NUMBER;
import static net.dunice.mk.rsmtelegrambot.handler.state.step.PartnerRegistrationStep.VALIDATE_PARTNER_NAME;

import lombok.RequiredArgsConstructor;
import net.dunice.mk.rsmtelegrambot.constant.Menu;
import net.dunice.mk.rsmtelegrambot.entity.Category;
import net.dunice.mk.rsmtelegrambot.entity.Partner;
import net.dunice.mk.rsmtelegrambot.events.PartnerRegisteredEvent;
import net.dunice.mk.rsmtelegrambot.handler.state.BasicState;
import net.dunice.mk.rsmtelegrambot.handler.state.stateobject.PartnerRegistrationState;
import net.dunice.mk.rsmtelegrambot.repository.CategoryRepository;
import net.dunice.mk.rsmtelegrambot.repository.PartnerRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.EnumMap;
import java.util.Map;

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
    public SendMessage handle(String message, Long telegramId) {
        PartnerRegistrationState state = partnerRegistrationStates.get(telegramId);
        if (state == null) {
            partnerRegistrationStates.put(telegramId, (state = new PartnerRegistrationState()));
        }
        return switch (state.getStep()) {
            case REQUEST_PARTNER_NAME -> {
                state.setStep(VALIDATE_PARTNER_NAME);
                yield generateSendMessage(telegramId, "Введите название партнёра:");
            }
            case VALIDATE_PARTNER_NAME -> {
                state.setName(message.trim());
                state.setStep(PHONE_NUMBER);
                yield generateSendMessage(telegramId, "Введите номер телефона:");
            }
            case PHONE_NUMBER -> {
                state.setPhoneNumber(message.trim());
                state.setStep(DISCOUNT_PERCENT);
                yield generateSendMessage(telegramId, "Введите процент скидки:");
            }
            case DISCOUNT_PERCENT -> {
                try {
                    state.setDiscountPercent(Short.parseShort(message.strip()));
                    state.setStep(CATEGORY);
                    yield generateSendMessage(telegramId, "Введите название категории:");
                } catch (NumberFormatException e) {
                    yield generateSendMessage(telegramId, "Процент скидки должен быть числом. Повторите ввод:");
                }
            }
            case CATEGORY -> {
                try {
                    Category category = categoryRepository.findByCategoryName(message);
                    state.setCategory(category);
                    state.setStep(LOGO);
                    yield generateSendMessage(telegramId, "Отправьте ваш логотип (изображение):");
                } catch (NumberFormatException e) {
                    yield generateSendMessage(telegramId, "Номер категории должен быть числом. Повторите ввод:");
                }
            }
            case LOGO -> {
                if (isImageMessage(message)) {
                    state.setLogo(message.getBytes());
                    state.setStep(DISCOUNT_DATE);
                    yield generateSendMessage(telegramId, "Введите дату конца действия скидки (ДД.ММ.ГГГГ):");
                } else {
                    yield generateSendMessage(telegramId, "Логотип должен быть изображением. Повторите ввод:");
                }
            }
            case DISCOUNT_DATE -> {
                try {
                    state.setDiscountDate(LocalDate.parse(message.trim(), DateTimeFormatter.ofPattern("dd.MM.yyyy")));
                    state.setStep(PARTNER_INFO);
                    yield generateSendMessage(telegramId, "Введите дополнительную информацию о партнёре:");
                } catch (DateTimeParseException e) {
                    yield generateSendMessage(telegramId, "Дата должна быть в формате ДД.ММ.ГГГГ. Повторите ввод:");
                }
            }
            case PARTNER_INFO -> {
                if (message.length() <= 255) {
                    state.setInfo(message.trim());
                    savePartner(state, telegramId);
                    state.setStep(FINISH);
                    yield generateSendMessage(telegramId, "Данные добавлены и отправлены на проверку.",
                        menus.get(GO_TO_MAIN_MENU));
                } else {
                    yield generateSendMessage(telegramId,
                        "Информация слишком длинная. Повторите ввод (до 255 символов):");
                }
            }
            case FINISH -> {
                if (TO_MAIN_MENU.equalsIgnoreCase(message)) {
                    partnerRegistrationStates.remove(telegramId);
                    basicStates.remove(telegramId);
                    yield generateSendMessage(telegramId,
                        "Регистрация завершена, профиль партнера станет доступен после проверки администратором");
                } else {
                    yield generateSendMessage(telegramId, "Неверная команда");
                }
            }
        };
    }

    private void savePartner(PartnerRegistrationState state, long telegramId) {
        Partner partner = new Partner();
        partner.setPartnerTelegramId(telegramId);
        partner.setName(state.getName());
        partner.setPhoneNumber(state.getPhoneNumber());
        partner.setDiscountPercent(state.getDiscountPercent());
        partner.setCategory(state.getCategory());
        partner.setLogo(state.getLogo());
        partner.setDiscountDate(LocalDateTime.of(state.getDiscountDate(), LocalTime.MIN));
        partner.setPartnersInfo(state.getInfo());
        partner.setValid(false);
        partnerRepository.save(partner);

        eventPublisher.publishEvent(new PartnerRegisteredEvent(partner));
    }

    private boolean isImageMessage(String message) {
        return message.startsWith("image:");
    }

    @Override
    public BasicState getState() {
        return PARTNER_REGISTRATION;
    }
}

