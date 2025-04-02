package net.dunice.mk.rsmtelegrambot.handler.messagehandler;

import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.TO_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.UPDATE_DISCOUNT_CODE_MENU;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.BasicStep.IN_PARTNER_MENU;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.BasicStep.REQUEST_VERIFICATION_CODE;

import lombok.RequiredArgsConstructor;
import net.dunice.mk.rsmtelegrambot.constant.Menu;
import net.dunice.mk.rsmtelegrambot.dto.MessageDto;
import net.dunice.mk.rsmtelegrambot.entity.Partner;
import net.dunice.mk.rsmtelegrambot.handler.MenuGenerator;
import net.dunice.mk.rsmtelegrambot.handler.state.BasicState;
import net.dunice.mk.rsmtelegrambot.repository.PartnerRepository;
import net.dunice.mk.rsmtelegrambot.service.DiscountCodeService;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;

import java.util.EnumMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RequestVerificationCodeHandler implements MessageHandler {

    private final Map<Long, BasicState> basicStates;
    private final EnumMap<Menu, ReplyKeyboard> menus;
    private final MenuGenerator menuGenerator;
    private final DiscountCodeService discountCodeService;
    private final PartnerRepository partnerRepository;

    @Override
    public BasicState.BasicStep getStep() {
        return REQUEST_VERIFICATION_CODE;
    }

    @Override
    public SendMessage handle(MessageDto messageDto, Long telegramId) {
        String text = messageDto.getText();

        if (TO_MAIN_MENU.equals(text)) {
            return goToMainMenu(telegramId);
        } else {
            return generateSendMessage(telegramId, """
                    Проверочный код: %06d
                    Оставшееся время действия кода(в секундах): %s
                    Если ваш проверочный код, и код у члена РСМ не совпадают, нажмите "Обновить код".
                    """.formatted(discountCodeService.getDiscountCode(), discountCodeService.getSecondsLeft()),
                menus.get(UPDATE_DISCOUNT_CODE_MENU));
        }
    }

    private SendMessage goToMainMenu(Long telegramId) {
        basicStates.get(telegramId).setStep(IN_PARTNER_MENU);
        Partner partner = partnerRepository.findById(telegramId).get();
        return generateSendMessage(telegramId, "Выберите раздел:", menuGenerator.getPartnerMenu(partner));
    }
}
