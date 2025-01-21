package net.dunice.mk.rsmtelegrambot.handler.messagehandler;

import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.CANCEL;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.TO_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.CANCEL_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.CATEGORY_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.GO_TO_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.BasicStep;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.BasicStep.IN_PARTNER_MENU;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.BasicStep.PARTNER_REGISTRATION;
import static net.dunice.mk.rsmtelegrambot.handler.state.PartnerRegistrationState.PartnerRegistrationStep.CATEGORY;
import static net.dunice.mk.rsmtelegrambot.handler.state.PartnerRegistrationState.PartnerRegistrationStep.DISCOUNT_DATE;
import static net.dunice.mk.rsmtelegrambot.handler.state.PartnerRegistrationState.PartnerRegistrationStep.DISCOUNT_PERCENT;
import static net.dunice.mk.rsmtelegrambot.handler.state.PartnerRegistrationState.PartnerRegistrationStep.FINISH;
import static net.dunice.mk.rsmtelegrambot.handler.state.PartnerRegistrationState.PartnerRegistrationStep.LOGO;
import static net.dunice.mk.rsmtelegrambot.handler.state.PartnerRegistrationState.PartnerRegistrationStep.PARTNER_INFO;
import static net.dunice.mk.rsmtelegrambot.handler.state.PartnerRegistrationState.PartnerRegistrationStep.PHONE_NUMBER;
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
    public SendMessage handle(MessageDto messageDto, Long telegramId) {
        String text = messageDto.getText();
        PartnerRegistrationState state = partnerRegistrationStates.get(telegramId);
        if (state == null) {
            partnerRegistrationStates.put(telegramId, (state = new PartnerRegistrationState()));
        }

        if (CANCEL.equals(text)) {
            basicStates.remove(telegramId);
            partnerRegistrationStates.remove(telegramId);
            return generateSendMessage(telegramId,
                "Регистрация отменена, введите /start, если хотите попробовать заново:");
        }

        return switch (state.getStep()) {
            case REQUEST_PARTNER_NAME -> {
                state.setStep(VALIDATE_PARTNER_NAME);
                yield generateSendMessage(telegramId, "Введите название партнёра:", menus.get(CANCEL_MENU));
            }
            case VALIDATE_PARTNER_NAME -> {
                state.setName(text.strip());
                state.setStep(PHONE_NUMBER);
                yield generateSendMessage(telegramId, "Введите номер телефона:", menus.get(CANCEL_MENU));
            }
            case PHONE_NUMBER -> {
                state.setPhoneNumber(text.strip());
                state.setStep(DISCOUNT_PERCENT);
                yield generateSendMessage(telegramId, "Введите процент скидки:", menus.get(CANCEL_MENU));
            }
            case DISCOUNT_PERCENT -> {
                try {
                    Short discountPercent = Short.parseShort(text.strip());
                    if (discountPercent >= 100 || discountPercent < 0) {
                        yield generateSendMessage(telegramId,
                            "Процент скидки должен быть от 0 до 100. Повторите ввод:", menus.get(CANCEL_MENU));
                    } else {
                        state.setDiscountPercent(discountPercent);
                        state.setStep(CATEGORY);
                        yield generateSendMessage(telegramId, "Выберите название категории:", menus.get(CATEGORY_MENU));
                    }
                } catch (NumberFormatException e) {
                    yield generateSendMessage(telegramId, "Процент скидки должен быть числом. Повторите ввод:",
                        menus.get(CANCEL_MENU));
                }
            }
            case CATEGORY -> {
                Optional<Category> category = categoryRepository.findByCategoryName(text);
                if (category.isPresent()) {
                    state.setCategory(category.get());
                    state.setStep(LOGO);
                    yield generateSendMessage(telegramId, "Отправьте ваш логотип (изображение):",
                        menus.get(CANCEL_MENU));
                } else {
                    yield generateSendMessage(telegramId, "Категория не найдена. Повторите ввод:",
                        menus.get(CANCEL_MENU));
                }
            }
            case LOGO -> {
                if (messageDto.getImage() != null) {
                    state.setLogo(messageDto.getImage());
                    state.setStep(DISCOUNT_DATE);
                    yield generateSendMessage(telegramId,
                        "Пожалуйста, введите дату и время конца действия скидки в формате (ДД.ММ.ГГГГ-ЧЧ:ММ) :",
                        menus.get(CANCEL_MENU));
                } else {
                    yield generateSendMessage(telegramId, "Логотип должен быть изображением. Повторите ввод:",
                        menus.get(CANCEL_MENU));
                }
            }
            case DISCOUNT_DATE -> {
                try {
                    state.setDiscountDate(
                        LocalDateTime.parse(text.trim(), DateTimeFormatter.ofPattern("dd.MM.yyyy-HH:mm")));
                    state.setStep(PARTNER_INFO);
                    yield generateSendMessage(telegramId, "Введите дополнительную информацию о партнёре:",
                        menus.get(CANCEL_MENU));
                } catch (DateTimeParseException e) {
                    yield generateSendMessage(telegramId,
                        "Дата должна быть в формате (ДД.ММ.ГГГГ-ЧЧ:ММ) . Повторите ввод:",
                        menus.get(CANCEL_MENU));
                }
            }
            case PARTNER_INFO -> {
                if (text.length() <= 255) {
                    state.setInfo(text.trim());
                    savePartner(state, telegramId);
                    state.setStep(FINISH);
                    yield generateSendMessage(telegramId, "Данные добавлены и отправлены на проверку.",
                        menus.get(GO_TO_MAIN_MENU));
                } else {
                    yield generateSendMessage(telegramId,
                        "Информация слишком длинная. Повторите ввод (до 255 символов):", menus.get(CANCEL_MENU));
                }
            }
            case FINISH -> {
                if (TO_MAIN_MENU.equalsIgnoreCase(text)) {
                    yield goToMainMenu(telegramId);
                } else {
                    yield generateSendMessage(telegramId, "Неверная команда.", menus.get(GO_TO_MAIN_MENU));
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
        partner.setDiscountDate(state.getDiscountDate());
        partner.setPartnersInfo(state.getInfo());
        partner.setValid(false);
        partnerRepository.save(partner);
        eventPublisher.publishEvent(new PartnerRegisteredEvent(partner));
    }

    @Override
    public BasicStep getStep() {
        return PARTNER_REGISTRATION;
    }

    private SendMessage goToMainMenu(Long telegramId) {
        partnerRegistrationStates.remove(telegramId);
        basicStates.get(telegramId).setStep(IN_PARTNER_MENU);
        return generateSendMessage(telegramId,
            "Регистрация завершена, профиль партнера станет доступен после проверки администратором");
    }
}

