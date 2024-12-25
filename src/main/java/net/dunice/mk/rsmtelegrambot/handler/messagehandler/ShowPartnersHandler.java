package net.dunice.mk.rsmtelegrambot.handler.messagehandler;

import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.PARTNERS_LIST;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.TO_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.IN_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.IN_PARTNER_MENU;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.SHOW_PARTNERS;
import static net.dunice.mk.rsmtelegrambot.handler.state.step.ShowPartnersStep.SHOW_PARTNERS_LIST;
import static net.dunice.mk.rsmtelegrambot.handler.state.step.ShowPartnersStep.SHOW_PARTNER_DETAILS;

import lombok.RequiredArgsConstructor;
import net.dunice.mk.rsmtelegrambot.constant.Menu;
import net.dunice.mk.rsmtelegrambot.dto.MessageDto;
import net.dunice.mk.rsmtelegrambot.entity.Partner;
import net.dunice.mk.rsmtelegrambot.handler.MenuGenerator;
import net.dunice.mk.rsmtelegrambot.handler.state.BasicState;
import net.dunice.mk.rsmtelegrambot.handler.state.step.ShowPartnersStep;
import net.dunice.mk.rsmtelegrambot.repository.PartnerRepository;
import net.dunice.mk.rsmtelegrambot.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
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
    private final Map<Menu, ReplyKeyboard> menus;
    private final Map<Long, BasicState> basicStates;
    private final UserRepository userRepository;
    private final Map<Long, ShowPartnersStep> showPartnerSteps;
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

    @Override
    public BasicState getState() {
        return SHOW_PARTNERS;
    }

    @Override
    public PartialBotApiMethod<Message> handle(MessageDto messageDto, Long telegramId) {
        String text = messageDto.getText();
        ShowPartnersStep step = showPartnerSteps.get(telegramId);
        if (step == null) {
            showPartnerSteps.put(telegramId, (step = ShowPartnersStep.SHOW_PARTNERS_LIST));
        }

        if (TO_MAIN_MENU.equalsIgnoreCase(text)) {
            return goToMainMenu(telegramId);
        }
        else if (PARTNERS_LIST.equalsIgnoreCase(text)) {
            showPartnerSteps.put(telegramId, SHOW_PARTNERS_LIST);
            step = SHOW_PARTNERS_LIST;
        }

        return switch (step) {
            case SHOW_PARTNERS_LIST -> {
                List<Partner> partners = partnerRepository.findValidPartnersWithPresentDiscount();
                showPartnerSteps.put(telegramId, SHOW_PARTNER_DETAILS);
                yield generateSendMessage(telegramId, "Партнеры РСМ: ",
                    getPartnersListKeyboard(partners));
            }
            case SHOW_PARTNER_DETAILS -> {
                Optional<Partner> partnerOptional = partnerRepository.findByName(text);
                if (partnerOptional.isPresent()) {
                    Partner partner = partnerOptional.get();
                    String partnerDescription = null;
                    if (isTgUserPartner(telegramId)) {
                        partnerDescription = PARTNER_INFO_FOR_PARTNERS.formatted(
                            partner.getName(),
                            partner.getCategory().getCategoryName(),
                            partner.getPartnersInfo(),
                            partner.getPhoneNumber());
                    }
                    else {
                        partnerDescription = PARTNER_INFO_FOR_USERS.formatted(
                            partner.getName(),
                            partner.getCategory().getCategoryName(),
                            partner.getPartnersInfo(),
                            partner.getPhoneNumber(),
                            partner.getDiscountPercent(),
                            partner.getDiscountDate() == null ? "Неограниченно" :
                                partner.getDiscountDate().toLocalDate());
                    }
                    byte[] logo = partner.getLogo();
                    yield isLogoPresent(logo)
                        ? generateImageMessage(telegramId, partnerDescription, getGoBackKeyboard(), logo)
                        : generateSendMessage(telegramId, partnerDescription, getGoBackKeyboard());
                } else {
                    yield generateSendMessage(telegramId, "Нет мероприятия с таким названием");
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
        InlineKeyboardButton toPartnersButton = new InlineKeyboardButton(PARTNERS_LIST);
        toPartnersButton.setCallbackData(toPartnersButton.getText());
        keyboard.add(List.of(toMainMenuButton));
        keyboard.add(List.of(toPartnersButton));
        inlineKeyboardMarkup.setKeyboard(keyboard);
        return inlineKeyboardMarkup;
    }

    private SendMessage goToMainMenu(Long telegramId) {
        showPartnerSteps.remove(telegramId);
        if (isTgUserPartner(telegramId)) {
            basicStates.put(telegramId, IN_PARTNER_MENU);
            return generateSendMessage(telegramId, "Партнеры РСМ:", menus.get(Menu.PARTNER_MAIN_MENU));
        }
        else {
            basicStates.put(telegramId, IN_MAIN_MENU);
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