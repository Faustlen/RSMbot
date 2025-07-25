package net.dunice.mk.rsmtelegrambot.handler.messagehandler;

import lombok.RequiredArgsConstructor;
import net.dunice.mk.rsmtelegrambot.constant.Menu;
import net.dunice.mk.rsmtelegrambot.dto.MessageDto;
import net.dunice.mk.rsmtelegrambot.entity.Partner;
import net.dunice.mk.rsmtelegrambot.entity.Stock;
import net.dunice.mk.rsmtelegrambot.entity.User;
import net.dunice.mk.rsmtelegrambot.handler.MenuGenerator;
import net.dunice.mk.rsmtelegrambot.handler.state.BasicState;
import net.dunice.mk.rsmtelegrambot.handler.state.ShowStocksState;
import net.dunice.mk.rsmtelegrambot.repository.PartnerRepository;
import net.dunice.mk.rsmtelegrambot.repository.StockRepository;
import net.dunice.mk.rsmtelegrambot.repository.UserRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.CANCEL;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.CHANGE_INFO;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.CHANGE_LOGO;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.CHANGE_NAME;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.CHANGE_PERIOD_END;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.CHANGE_PERIOD_START;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.DELETE_STOCK;  // Добавьте в ButtonName
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.EDIT_STOCK;    // Добавьте в ButtonName
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.GO_TO_PARTNER;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.STOCKS_LIST;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.TO_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.NO;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.YES;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.CANCEL_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.GO_TO_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.OK_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.SELECTION_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.STOCK_FIELDS_MENU;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.BasicStep.IN_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.BasicStep.IN_PARTNER_MENU;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.BasicStep.SHOW_PARTNERS;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.BasicStep.SHOW_STOCKS;
import static net.dunice.mk.rsmtelegrambot.handler.state.ShowEventsState.ShowEventsStep.SHOW_EVENT_DETAILS;
import static net.dunice.mk.rsmtelegrambot.handler.state.ShowStocksState.ShowStocksStep.*;

@Service
@RequiredArgsConstructor
public class ShowStocksHandler implements MessageHandler {

    private static final String STOCK_INFO_TEMPLATE = """
        Название: %s
        Описание: %s
        Период действия:
        %s - %s
        """;

    private final MenuGenerator menuGenerator;
    private final StockRepository stockRepository;
    private final UserRepository userRepository;
    private final PartnerRepository partnerRepository;
    private final Map<Long, BasicState> basicStates;
    private final Map<Long, ShowStocksState> showStocksStates;
    private final Map<?, ReplyKeyboard> menus;

    private ShowPartnersHandler showPartnersHandler;

    @Autowired
    public void setShowPartnersHandler(@Lazy ShowPartnersHandler showPartnersHandler) {
        this.showPartnersHandler = showPartnersHandler;
    }

    @Override
    public BasicState.BasicStep getStep() {
        return SHOW_STOCKS;
    }

    @Override
    public PartialBotApiMethod<Message> handle(MessageDto messageDto, Long telegramId) {
        ShowStocksState state = showStocksStates.get(telegramId);
        if (state == null) {
            state = new ShowStocksState();
            showStocksStates.put(telegramId, state);
        }

        if (TO_MAIN_MENU.equalsIgnoreCase(messageDto.getText())) {
            return goToMainMenu(telegramId);
        }

        return switch (state.getStep()) {
            case SHOW_STOCKS_LIST -> handleShowStocksList(telegramId, state);
            case SHOW_STOCK_DETAILS -> handleShowStockDetails(messageDto, telegramId, state);
            case HANDLE_USER_ACTION -> handleUserAction(messageDto, telegramId, state);
            case SELECT_STOCK_FIELD -> handleSelectStockField(messageDto, telegramId, state);
            case EDIT_STOCK_FIELD -> handleEditStockField(messageDto, telegramId, state);
            case CONFIRM_STOCK_EDIT -> handleConfirmStockEdit(messageDto, telegramId, state);
            case CONFIRM_STOCK_DELETE -> handleConfirmStockDelete(messageDto, telegramId, state);
        };
    }

    public PartialBotApiMethod<Message> handlePartnerStocks(MessageDto messageDto, Long telegramId) {
        ShowStocksState state = showStocksStates.get(telegramId);
        if (state == null) {
            state = new ShowStocksState();
            showStocksStates.put(telegramId, state);
        }
        state.setStep(SHOW_STOCK_DETAILS);

        Optional<Partner> partnerOptional = partnerRepository.findById(Long.parseLong(messageDto.getText()));
        List<Stock> stocks = stockRepository.findByPartnerTelegramId(partnerOptional.get());

        return generateSendMessage(
            telegramId,
            "Выберите интересующую вас акцию:",
            generateStocksListKeyboard(stocks)
        );
    }

    private PartialBotApiMethod<Message> handleShowStocksList(Long telegramId, ShowStocksState state) {
        List<Stock> stocks;

        Optional<Partner> partnerOptional = partnerRepository.findById(telegramId);
        if (partnerOptional.isPresent()) {
            stocks = stockRepository.findByPartnerTelegramId(partnerOptional.get());
        } else if (basicStates.get(telegramId).getUser().getUserRole().name().equals("USER")) {
            stocks = stockRepository.findAllCurrentStocks();
        } else {
            stocks = stockRepository.findAll();
        }
        state.setStep(SHOW_STOCK_DETAILS);

        return generateSendMessage(
            telegramId,
            "Выберите интересующую вас акцию:",
            generateStocksListKeyboard(stocks)
        );
    }

    private PartialBotApiMethod<Message> handleShowStockDetails(
        MessageDto messageDto, Long telegramId, ShowStocksState state
    ) {
        String text = messageDto.getText();
        if (text == null) {
            return handleShowStocksList(telegramId, state);
        }

        try {
            Integer stockId = Integer.valueOf(text.substring(text.lastIndexOf(' ') + 1));
            Optional<Stock> stockOptional = stockRepository.findById(stockId);
            if (stockOptional.isEmpty()) {
                return generateSendMessage(telegramId, "Нет акции с таким ID.");
            }

            Stock targetStock = stockOptional.get();
            state.setTargetStock(targetStock);
            state.setStep(HANDLE_USER_ACTION);

            String stockDescription = getStockDescription(targetStock);

            byte[] image = targetStock.getImage();
            if (isLogoPresent(image)) {
                SendPhoto sendPhoto = generateImageMessage(
                    telegramId,
                    stockDescription,
                    getUserActionKeyboard(telegramId, state),
                    image
                );
                sendPhoto.setParseMode("HTML");
                return sendPhoto;
            } else {
                SendMessage sendMessage = generateSendMessage(
                    telegramId,
                    stockDescription,
                    getUserActionKeyboard(telegramId, state)
                );
                sendMessage.setParseMode("HTML");
                return sendMessage;
            }
        } catch (NumberFormatException e) {
            return generateSendMessage(telegramId, "Неверный формат ID акции.");
        }
    }

    private PartialBotApiMethod<Message> handleUserAction(MessageDto messageDto, Long telegramId, ShowStocksState state) {
        String text = messageDto.getText();
        return switch (text) {
            case STOCKS_LIST -> {
                state.setStep(SHOW_STOCKS_LIST);
                yield handleShowStocksList(telegramId, state);
            }
            case EDIT_STOCK -> {
                state.setStep(SELECT_STOCK_FIELD);
                yield generateSendMessage(
                    telegramId,
                    "Выберите поле, которое хотите отредактировать:",
                    menus.get(STOCK_FIELDS_MENU)
                );
            }
            case DELETE_STOCK -> {
                state.setStep(CONFIRM_STOCK_DELETE);
                yield generateSendMessage(
                    telegramId,
                    String.format("Вы точно хотите удалить акцию '%s'?", state.getTargetStock().getHead()),
                    menus.get(SELECTION_MENU)
                );
            }
            case GO_TO_PARTNER -> {
                basicStates.get(telegramId).setStep(SHOW_PARTNERS);
                messageDto.setText(state.getTargetStock().getPartnerTelegramId().getName());
                yield showPartnersHandler.handleDetails(messageDto, telegramId);
            }
            default -> {
                yield generateSendMessage(telegramId, "Неверная команда", menus.get(GO_TO_MAIN_MENU));
            }
        };
    }


    private PartialBotApiMethod<Message> handleSelectStockField(MessageDto messageDto, Long telegramId, ShowStocksState state) {
        String text = messageDto.getText();

        if (CANCEL.equalsIgnoreCase(text) || text == null) {
            state.setStep(SHOW_STOCK_DETAILS);
            messageDto.setText(" " + state.getTargetStock().getStocksId());
            return handleShowStockDetails(messageDto, telegramId, state);
        }

        if (!isFieldName(text)) {
            return generateSendMessage(
                telegramId,
                "Неверное поле. Выберите одно из доступных.",
                menus.get(STOCK_FIELDS_MENU)
            );
        }

        state.setEditingFieldName(text);
        state.setStep(EDIT_STOCK_FIELD);

        String question = switch (text) {
            case CHANGE_NAME -> "Введите новое название акции:";
            case CHANGE_INFO -> "Введите новое описание акции:";
            case CHANGE_PERIOD_START -> "Введите новую дату начала в формате (ДД.ММ.ГГГГ):";
            case CHANGE_PERIOD_END -> "Введите новую дату окончания в формате (ДД.ММ.ГГГГ):";
            case CHANGE_LOGO -> "Отправьте новое изображение (логотип) акции:";
            default -> "Поле неизвестно. Попробуйте снова.";
        };

        return generateSendMessage(telegramId, question, menus.get(CANCEL_MENU));
    }

    private PartialBotApiMethod<Message> handleEditStockField(MessageDto messageDto, Long telegramId, ShowStocksState state) {
        String text = messageDto.getText();
        if (CANCEL.equalsIgnoreCase(text) || text == null) {
            state.setStep(SHOW_STOCK_DETAILS);
            messageDto.setText(" " + state.getTargetStock().getStocksId());
            return handleShowStockDetails(messageDto, telegramId, state);
        }

        Stock targetStock = state.getTargetStock();

        if (StringUtils.isBlank(text) && messageDto.getImage() == null) {
            state.setStep(SHOW_STOCK_DETAILS);
            messageDto.setText("%s | ID: %s".formatted(targetStock.getHead(), targetStock.getStocksId()));
            return handleShowStockDetails(messageDto, telegramId, state);
        }

        try {
            switch (state.getEditingFieldName()) {
                case CHANGE_NAME -> {
                    if (text.length() > 100) {
                        return generateSendMessage(telegramId, "Название не более 100 символов!", menus.get(CANCEL_MENU));
                    }
                    targetStock.setHead(text.trim());
                }
                case CHANGE_INFO -> {
                    if (text.length() > 250) {
                        return generateSendMessage(telegramId, "Описание не более 250 символов!", menus.get(CANCEL_MENU));
                    }
                    targetStock.setDescription(text.trim());
                }
                case CHANGE_PERIOD_START -> {
                    LocalDate startDate = LocalDate.parse(text.trim(), DateTimeFormatter.ofPattern("dd.MM.yyyy"));
                    targetStock.setPeriodStocksStart(startDate);
                }
                case CHANGE_PERIOD_END -> {
                    LocalDate endDate = LocalDate.parse(text.trim(), DateTimeFormatter.ofPattern("dd.MM.yyyy"));
                    targetStock.setPeriodStocksEnd(endDate);
                }
                case CHANGE_LOGO -> {
                    if (messageDto.getImage() == null) {
                        return generateSendMessage(telegramId, "Нужно прислать изображение.", menus.get(CANCEL_MENU));
                    }
                    targetStock.setImage(messageDto.getImage());
                }
            }
        } catch (DateTimeParseException e) {
            return generateSendMessage(
                telegramId,
                "Ошибка парсинга даты. Используйте формат ДД.ММ.ГГГГ.",
                menus.get(CANCEL_MENU)
            );
        } catch (Exception e) {
            return generateSendMessage(
                telegramId,
                "Ошибка при редактировании поля.",
                menus.get(CANCEL_MENU)
            );
        }

        if (targetStock.getPeriodStocksEnd().isBefore(targetStock.getPeriodStocksStart())) {
            return generateSendMessage(telegramId, "Дата окончания не может быть раньше даты начала.", menus.get(SELECTION_MENU));
        }

        state.setStep(CONFIRM_STOCK_EDIT);
        return buildStockConfirmMessage(telegramId, targetStock);
    }

    private PartialBotApiMethod<Message> handleConfirmStockEdit(MessageDto messageDto, Long telegramId, ShowStocksState state) {
        String text = messageDto.getText();
        if (YES.equalsIgnoreCase(text)) {
            stockRepository.save(state.getTargetStock());
        } else if (!NO.equalsIgnoreCase(text)) {
            return buildStockConfirmMessage(telegramId, state.getTargetStock());
        }
        state.setStep(EDIT_STOCK_FIELD);
        messageDto.setText("%s | ID: %s".formatted(state.getTargetStock().getHead(), state.getTargetStock().getStocksId()));
        return handleShowStockDetails(messageDto, telegramId, state);
    }

    private PartialBotApiMethod<Message> handleConfirmStockDelete(MessageDto messageDto, Long telegramId, ShowStocksState state) {
        String text = messageDto.getText();
        if (YES.equalsIgnoreCase(text)) {
            stockRepository.delete(state.getTargetStock());
            String resp = "Акция '%s' удалена.".formatted(state.getTargetStock().getHead());
            state.setStep(SHOW_STOCKS_LIST);
            return generateSendMessage(telegramId, resp, menus.get(OK_MENU));
        } else if (NO.equalsIgnoreCase(text)) {
            String resp = "Удаление акции отменено.";
            state.setStep(SHOW_STOCKS_LIST);
            messageDto.setText("%s | ID: %s".formatted(state.getTargetStock().getHead(), state.getTargetStock().getStocksId()));
            return generateSendMessage(telegramId, resp, menus.get(OK_MENU));
        } else {
            return generateSendMessage(
                telegramId,
                "Неверная команда. Подтвердите или отмените удаление.",
                menus.get(SELECTION_MENU)
            );
        }
    }

    private SendMessage goToMainMenu(Long telegramId) {
        showStocksStates.remove(telegramId);

        if (partnerRepository.existsById(telegramId)) {
            basicStates.get(telegramId).setStep(IN_PARTNER_MENU);
            Partner partner = partnerRepository.findById(telegramId).get();
            return generateSendMessage(
                telegramId,
                "Выберите раздел:",
                menuGenerator.getPartnerMenu(partner)
            );
        } else {
            basicStates.get(telegramId).setStep(IN_MAIN_MENU);
            return menuGenerator.generateRoleSpecificMainMenu(
                telegramId,
                userRepository.findByTelegramId(telegramId).get().getUserRole()
            );
        }
    }

    private String getStockDescription(Stock stock) {
        return STOCK_INFO_TEMPLATE.formatted(
            stock.getHead(),
            stock.getDescription(),
            stock.getPeriodStocksStart().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
            stock.getPeriodStocksEnd().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        );
    }

    private ReplyKeyboardMarkup generateStocksListKeyboard(List<Stock> stocks) {
        ReplyKeyboardMarkup replyMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow mainMenuRow = new KeyboardRow();
        mainMenuRow.add(TO_MAIN_MENU);
        keyboard.add(mainMenuRow);

        for (Stock stock : stocks) {
            KeyboardRow row = new KeyboardRow();
            row.add("%s | ID: %s".formatted(stock.getHead(), stock.getStocksId()));
            keyboard.add(row);
        }

        replyMarkup.setKeyboard(keyboard);
        replyMarkup.setResizeKeyboard(true);
        replyMarkup.setOneTimeKeyboard(false);
        return replyMarkup;
    }

    private boolean isLogoPresent(byte[] logo) {
        return logo != null && logo.length > 0;
    }

    private InlineKeyboardMarkup getUserActionKeyboard(Long telegramId, ShowStocksState state) {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        InlineKeyboardButton mainMenuBtn = new InlineKeyboardButton(TO_MAIN_MENU);
        mainMenuBtn.setCallbackData(TO_MAIN_MENU);
        rows.add(List.of(mainMenuBtn));

        InlineKeyboardButton listStocksBtn = new InlineKeyboardButton(STOCKS_LIST);
        listStocksBtn.setCallbackData(STOCKS_LIST);
        rows.add(List.of(listStocksBtn));

        if (!telegramId.equals(state.getTargetStock().getPartnerTelegramId().getPartnerTelegramId())) {
            InlineKeyboardButton gotoPartner = new InlineKeyboardButton(GO_TO_PARTNER);
            gotoPartner.setCallbackData(GO_TO_PARTNER);
            rows.add(List.of(gotoPartner));
        }

        User user = userRepository.findById(telegramId).orElse(null);
        if ((user != null && user.getUserRole() != null && !user.getUserRole().name().equals("USER")) ||
            telegramId.equals(state.getTargetStock().getPartnerTelegramId().getPartnerTelegramId())) {
            InlineKeyboardButton deleteBtn = new InlineKeyboardButton(DELETE_STOCK);
            InlineKeyboardButton editBtn = new InlineKeyboardButton(EDIT_STOCK);
            deleteBtn.setCallbackData(DELETE_STOCK);
            editBtn.setCallbackData(EDIT_STOCK);
            rows.add(List.of(deleteBtn, editBtn));
        }

        inlineKeyboard.setKeyboard(rows);
        return inlineKeyboard;
    }

    private boolean isFieldName(String text) {
        if (text == null) return false;
        return List.of(
            CHANGE_NAME,
            CHANGE_INFO,
            GO_TO_PARTNER,
            CHANGE_PERIOD_START,
            CHANGE_PERIOD_END,
            CHANGE_LOGO,
            CANCEL)
            .contains(text);
    }

    private PartialBotApiMethod<Message> buildStockConfirmMessage(Long telegramId, Stock stock) {
        String updatedInfo = "Акция с новыми данными:\n" +
            getStockDescription(stock) +
            "\nСохранить изменения?";

        if (isLogoPresent(stock.getImage())) {
            SendPhoto sendPhoto = generateImageMessage(telegramId, updatedInfo, menus.get(SELECTION_MENU), stock.getImage());
            sendPhoto.setParseMode("HTML");
            return sendPhoto;
        } else {
            SendMessage sendMessage = generateSendMessage(telegramId, updatedInfo, menus.get(SELECTION_MENU));
            sendMessage.setParseMode("HTML");
            return sendMessage;
        }
    }
}
