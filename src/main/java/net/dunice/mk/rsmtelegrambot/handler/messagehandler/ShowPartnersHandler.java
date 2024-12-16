package net.dunice.mk.rsmtelegrambot.handler.messagehandler;

import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.TO_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.TO_PARTNERS_LIST;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.IN_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.SHOW_PARTNERS;
import static net.dunice.mk.rsmtelegrambot.handler.state.step.ShowPartnersStep.FINISH;
import static net.dunice.mk.rsmtelegrambot.handler.state.step.ShowPartnersStep.SHOW_PARTNERS_LIST;
import static net.dunice.mk.rsmtelegrambot.handler.state.step.ShowPartnersStep.SHOW_PARTNER_DETAILS;

import lombok.RequiredArgsConstructor;
import net.dunice.mk.rsmtelegrambot.entity.Partner;
import net.dunice.mk.rsmtelegrambot.handler.MenuGenerator;
import net.dunice.mk.rsmtelegrambot.handler.state.BasicState;
import net.dunice.mk.rsmtelegrambot.handler.state.step.ShowPartnersStep;
import net.dunice.mk.rsmtelegrambot.repository.PartnerRepository;
import net.dunice.mk.rsmtelegrambot.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ShowPartnersHandler implements MessageHandler {

    private final PartnerRepository partnerRepository;
    private final MenuGenerator menuGenerator;
    private final Map<Long, BasicState> basicStates;
    private final UserRepository userRepository;
    private final Map<Long, ShowPartnersStep> showPartnerSteps;
    private static final String DESCRIPTION_TEMPLATE = """
        Партнер - %s
        Категория - %s
        Информация о партнере - %s
        Номер телефона - %s
        Процент скидки - %s%%
        Дата окончания скидки - %s
        """;

    @Override
    public BasicState getState() {
        return SHOW_PARTNERS;
    }

    @Override
    public PartialBotApiMethod<Message> handle(String message, Long telegramId) {

        ShowPartnersStep step = showPartnerSteps.get(telegramId);
        if (step == null) {
            showPartnerSteps.put(telegramId, (step = ShowPartnersStep.SHOW_PARTNERS_LIST));
        }

        return switch (step) {
            case SHOW_PARTNERS_LIST -> {
                List<Partner> partners = partnerRepository.findAll();
                showPartnerSteps.put(telegramId, SHOW_PARTNER_DETAILS);
                yield generateSendMessage(telegramId, "Партнеры: ",
                    getPartnersListKeyboard(partners));
            }
            case SHOW_PARTNER_DETAILS -> {
                if (TO_MAIN_MENU.equalsIgnoreCase(message)) {
                    showPartnerSteps.remove(telegramId);
                    basicStates.put(telegramId, IN_MAIN_MENU);
                    yield menuGenerator.generateRoleSpecificMainMenu(telegramId,
                        userRepository.findByTelegramId(telegramId).get().getUserRole());
                } else {
                    Optional<Partner> partnerOptional = partnerRepository.findByName(message);
                    if (partnerOptional.isPresent()) {
                        Partner partner = partnerOptional.get();
                        String partnerDescription = String.format(DESCRIPTION_TEMPLATE,
                            partner.getName(),
                            partner.getCategoryId().getCategoryName(),
                            partner.getPartnersInfo(),
                            partner.getPhoneNumber(),
                            partner.getDiscountPercent(),
                            partner.getDiscountDate() == null ? "Неограниченно" :
                                partner.getDiscountDate().toLocalDate());
                        showPartnerSteps.put(telegramId, FINISH);
                        byte[] logo = partner.getLogo();
                        yield isLogoPresent(logo)
                            ? generateImageMessage(telegramId, partnerDescription, getGoBackKeyboard(), logo)
                            : generateSendMessage(telegramId, partnerDescription, getGoBackKeyboard());
                    } else {
                        yield generateSendMessage(telegramId, "Нет мероприятия с таким названием");
                    }
                }
            }
            case FINISH -> {
                if (TO_MAIN_MENU.equalsIgnoreCase(message)) {
                    showPartnerSteps.remove(telegramId);
                    basicStates.put(telegramId, IN_MAIN_MENU);
                    yield menuGenerator.generateRoleSpecificMainMenu(telegramId,
                        userRepository.findByTelegramId(telegramId).get().getUserRole());
                } else if (TO_PARTNERS_LIST.equalsIgnoreCase(message)) {
                    showPartnerSteps.put(telegramId, SHOW_PARTNERS_LIST);
                    yield handle(message, telegramId);
                } else {
                    yield generateSendMessage(telegramId, "Неверная команда");
                }
            }
        };
    }

    private ReplyKeyboardMarkup getPartnersListKeyboard(List<Partner> partners) {
        ReplyKeyboardMarkup replyMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow firstRow = new KeyboardRow();
        firstRow.add(TO_MAIN_MENU);
        keyboard.add(firstRow);
        for (int i = 0; i < partners.size(); ) {
            KeyboardRow row = new KeyboardRow();
            row.add(partners.get(i++).getName());
            keyboard.add(row);
        }
        replyMarkup.setKeyboard(keyboard);
        replyMarkup.setResizeKeyboard(true);
        replyMarkup.setOneTimeKeyboard(false);
        return replyMarkup;
    }

    private ReplyKeyboard getGoBackKeyboard() {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        InlineKeyboardButton toMainMenuButton = new InlineKeyboardButton(TO_MAIN_MENU);
        toMainMenuButton.setCallbackData(toMainMenuButton.getText());
        InlineKeyboardButton toPartnersButton = new InlineKeyboardButton(TO_PARTNERS_LIST);
        toPartnersButton.setCallbackData(toPartnersButton.getText());
        keyboard.add(List.of(toMainMenuButton));
        keyboard.add(List.of(toPartnersButton));
        inlineKeyboardMarkup.setKeyboard(keyboard);
        return inlineKeyboardMarkup;
    }

    private boolean isLogoPresent(byte[] logo) {
        return logo != null && logo.length > 0;
    }
}