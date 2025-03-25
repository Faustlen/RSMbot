package net.dunice.mk.rsmtelegrambot.handler.messagehandler;

import lombok.RequiredArgsConstructor;
import net.dunice.mk.rsmtelegrambot.dto.MessageDto;
import net.dunice.mk.rsmtelegrambot.entity.Stock;
import net.dunice.mk.rsmtelegrambot.handler.MenuGenerator;
import net.dunice.mk.rsmtelegrambot.handler.state.BasicState;
import net.dunice.mk.rsmtelegrambot.handler.state.ShowStocksState;
import net.dunice.mk.rsmtelegrambot.repository.StockRepository;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import net.dunice.mk.rsmtelegrambot.constant.Menu;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.STOCKS_LIST;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.TO_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.BasicStep.IN_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.BasicStep.SHOW_STOCKS;
import static net.dunice.mk.rsmtelegrambot.handler.state.ShowStocksState.ShowStocksStep.HANDLE_USER_ACTION;
import static net.dunice.mk.rsmtelegrambot.handler.state.ShowStocksState.ShowStocksStep.SHOW_STOCKS_LIST;
import static net.dunice.mk.rsmtelegrambot.handler.state.ShowStocksState.ShowStocksStep.SHOW_STOCK_DETAILS;

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
    private final Map<Long, BasicState> basicStates;
    private final Map<Long, ShowStocksState> showStocksStates;
    private final Map<Menu, ReplyKeyboard> menus;

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
        };
    }

    private PartialBotApiMethod<Message> handleShowStocksList(Long telegramId, ShowStocksState state) {

        List<Stock> stocks = stockRepository.findAllBetweenPeriodStocksStartAndBetweenByPeriodStocksEndOrderByPeriodStocksEndAsc(LocalDate.now());
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

        if(text == null) {
            return handleShowStocksList(telegramId, state);
        }

        Integer stockId = Integer.valueOf(text.substring(text.lastIndexOf(' ') + 1));
        Optional<Stock> stockOptional = stockRepository.findById(stockId);
        if (stockOptional.isEmpty()) {
            return generateSendMessage(telegramId, "Нет акции с таким названием.");
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
                getUserActionKeyboard(),
                image
            );
            sendPhoto.setParseMode("HTML");
            return sendPhoto;
        } else {
            SendMessage sendMessage = generateSendMessage(
                telegramId,
                stockDescription,
                getUserActionKeyboard()
            );
            sendMessage.setParseMode("HTML");
            return sendMessage;
        }
    }

    private PartialBotApiMethod<Message> handleUserAction(
            MessageDto messageDto, Long telegramId, ShowStocksState state
    ) {
        String text = messageDto.getText();

        if (STOCKS_LIST.equalsIgnoreCase(text)) {
            state.setStep(SHOW_STOCKS_LIST);
            return handleShowStocksList(telegramId, state);
        }
        return goToMainMenu(telegramId);
    }

    private ReplyKeyboardMarkup generateStocksListKeyboard(List<Stock> stocks) {
        ReplyKeyboardMarkup replyMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow mainMenuRow = new KeyboardRow();
        mainMenuRow.add(TO_MAIN_MENU);
        keyboard.add(mainMenuRow);

        for (Stock stock : stocks) {
            KeyboardRow row = new KeyboardRow();
            row.add("%s | ID: %s".formatted(
                    stock.getHead(),
                    stock.getStocksId())
            );
            keyboard.add(row);
        }

        replyMarkup.setKeyboard(keyboard);
        replyMarkup.setResizeKeyboard(true);
        replyMarkup.setOneTimeKeyboard(false);
        return replyMarkup;
    }

    private ReplyKeyboard getUserActionKeyboard() {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        InlineKeyboardButton toMainMenuButton = new InlineKeyboardButton(TO_MAIN_MENU);
        toMainMenuButton.setCallbackData(TO_MAIN_MENU);

        InlineKeyboardButton toEventsButton = new InlineKeyboardButton(STOCKS_LIST);
        toEventsButton.setCallbackData(STOCKS_LIST);

        keyboard.add(List.of(toMainMenuButton));
        keyboard.add(List.of(toEventsButton));

        inlineKeyboardMarkup.setKeyboard(keyboard);
        return inlineKeyboardMarkup;
    }

    private String getStockDescription(Stock targetStock) {
        return STOCK_INFO_TEMPLATE.formatted(
            targetStock.getHead(),
            targetStock.getDescription(),
            targetStock.getPeriodStocksStart().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
            targetStock.getPeriodStocksEnd().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        );
    }

    private SendMessage goToMainMenu(Long telegramId) {
        BasicState state = basicStates.get(telegramId);
        showStocksStates.remove(telegramId);
        state.setStep(IN_MAIN_MENU);
        return menuGenerator.generateRoleSpecificMainMenu(telegramId, state.getUser().getUserRole());
    }

    private boolean isLogoPresent(byte[] logo) {
        return logo != null && logo.length > 0;
    }
}
