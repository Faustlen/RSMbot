package net.dunice.mk.rsmtelegrambot.handler.messagehandler;

import lombok.RequiredArgsConstructor;
import net.dunice.mk.rsmtelegrambot.constant.Menu;
import net.dunice.mk.rsmtelegrambot.dto.MessageDto;
import net.dunice.mk.rsmtelegrambot.entity.Check;
import net.dunice.mk.rsmtelegrambot.handler.state.BasicState;
import net.dunice.mk.rsmtelegrambot.handler.state.CreateCheckState;
import net.dunice.mk.rsmtelegrambot.repository.CheckRepository;
import net.dunice.mk.rsmtelegrambot.repository.PartnerRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.CANCEL;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.NO;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.TO_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.YES;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.CANCEL_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.GO_TO_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.SELECTION_MENU;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.BasicStep.CREATE_CHECK;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.BasicStep.IN_PARTNER_MENU;
import static net.dunice.mk.rsmtelegrambot.handler.state.CreateCheckState.CreateCheckStep.CONFIRM_CHECK_CREATION;
import static net.dunice.mk.rsmtelegrambot.handler.state.CreateCheckState.CreateCheckStep.CONFIRM_MEMBERSHIP;

@Service
@RequiredArgsConstructor
public class CreateCheckHandler implements MessageHandler {

    private final Map<Long, BasicState> basicStates;
    private final Map<Long, CreateCheckState> createCheckStates;
    private final Map<Menu, ReplyKeyboard> menus;
    private final PartnerRepository partnerRepository;
    private final CheckRepository checkRepository;

    @Override
    public BasicState.BasicStep getStep() {
        return CREATE_CHECK;
    }

    @Override
    public SendMessage handle(MessageDto messageDto, Long telegramId) {
        String text = messageDto.getText();
        CreateCheckState state = createCheckStates.get(telegramId);
        if (state == null) {
            createCheckStates.put(telegramId, (state = new CreateCheckState()));
            state.setPartner(partnerRepository.findById(telegramId).get());
            if (!state.getPartner().isValid()) {
                return  generateSendMessage(telegramId, "Ваш профиль не подтвержден администратором.",
                        menus.get(GO_TO_MAIN_MENU));
            }
        }

        if (StringUtils.equalsAny(text, TO_MAIN_MENU, CANCEL)) {
            return goToMainMenu(telegramId);
        }

        return switch (state.getStep()) {
            case DISCOUNT_CALC -> handleDiscountCalc(state, telegramId);
            case ENTER_SUM -> handleEnterSum(state, telegramId, text);
            case CONFIRM_MEMBERSHIP -> handleConfirmMembership(state, telegramId, text);
            case CONFIRM_CHECK_CREATION -> handleConfirmCheckCreation(state, telegramId, text);
        };
    }

    private SendMessage handleDiscountCalc(CreateCheckState state, Long telegramId) {
        short discountPercent = (state.getPartner().getDiscountDate().isAfter(LocalDateTime.now()))
                ? state.getPartner().getDiscountPercent()
                : 0;
        state.setDiscountPercent(discountPercent);
        state.setStep(CreateCheckState.CreateCheckStep.ENTER_SUM);
        return generateSendMessage(telegramId,
                "Процент предоставляемой скидки: " + discountPercent + "%\nВведите сумму чека:", menus.get(CANCEL_MENU));
    }

    private SendMessage handleEnterSum(CreateCheckState state, Long telegramId, String text) {
        try {
            BigDecimal checkSum = new BigDecimal(text.strip());
            if (checkSum.precision() - checkSum.scale() > 6) {
                return generateSendMessage(telegramId,
                        "Сумма не должна превышать 7 целых знаков.", menus.get(CANCEL_MENU));
            }
            state.setCheckSum(checkSum);
            state.setStep(CONFIRM_MEMBERSHIP);
            return generateSendMessage(telegramId, "Пользователь подтвердил членство в РСМ?", menus.get(SELECTION_MENU));
        } catch (NumberFormatException e) {
            return generateSendMessage(telegramId,
                    "Неверный формат суммы. Введите число в формате '123.45':", menus.get(CANCEL_MENU));
        }
    }

    private SendMessage handleConfirmMembership(CreateCheckState state, Long telegramId, String text) {
        if (YES.equalsIgnoreCase(text)) {
            state.setConfirmationDate(LocalDateTime.now());
            state.setStep(CONFIRM_CHECK_CREATION);
            return generateSendMessage(telegramId, String.format("""
                    Сумма чека: %.2f
                    Суммачека с учётом скидки: %.2f
                    Процент скидки: %d%%
                    Дата подтверждения: %s
                    Сформировать чек?
                    """,
                    state.getCheckSum(),
                state.getCheckSum()
                    .multiply(BigDecimal.valueOf(100 - state.getDiscountPercent())
                    .divide(BigDecimal.valueOf(100))),
                    state.getDiscountPercent(),
                    state.getConfirmationDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"))
            ), menus.get(SELECTION_MENU));
        } else if (NO.equalsIgnoreCase(text)) {
            return generateSendMessage(telegramId, "Членство не подтверждено.", menus.get(GO_TO_MAIN_MENU));
        }
        return generateSendMessage(telegramId, "Пользователь подтвердил членство в РСМ?", menus.get(SELECTION_MENU));
    }

    private SendMessage handleConfirmCheckCreation(CreateCheckState state, Long telegramId, String text) {
        if (YES.equalsIgnoreCase(text)) {
            saveCheck(state);
            createCheckStates.remove(telegramId);
            return generateSendMessage(telegramId, "Чек успешно сохранён!", menus.get(GO_TO_MAIN_MENU));
        } else {
            return generateSendMessage(telegramId, "Создание чека отменено.", menus.get(GO_TO_MAIN_MENU));
        }
    }

    private SendMessage goToMainMenu(Long telegramId) {
        basicStates.get(telegramId).setStep(IN_PARTNER_MENU);
        createCheckStates.remove(telegramId);
        return generateSendMessage(telegramId, "Выберите раздел:", menus.get(Menu.PARTNER_MAIN_MENU));
    }

    private void saveCheck(CreateCheckState state) {
        Check check = new Check();
        check.setPartnerTelegramId(state.getPartner());
        check.setCheckSum(state.getCheckSum());
        check.setDiscountPercent(state.getDiscountPercent());
        check.setDate(state.getConfirmationDate());
        checkRepository.save(check);
    }
}