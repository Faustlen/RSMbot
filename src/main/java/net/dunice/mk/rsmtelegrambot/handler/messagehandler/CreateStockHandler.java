package net.dunice.mk.rsmtelegrambot.handler.messagehandler;

import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.CANCEL;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.NO;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.SKIP;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.TO_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.YES;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.CANCEL_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.GO_TO_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.SELECTION_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.SKIP_MENU;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.BasicStep.*;
import static net.dunice.mk.rsmtelegrambot.handler.state.CreateStockState.StockCreationStep.CONFIRM_STOCK;

import lombok.RequiredArgsConstructor;
import net.dunice.mk.rsmtelegrambot.constant.Menu;
import net.dunice.mk.rsmtelegrambot.dto.MessageDto;
import net.dunice.mk.rsmtelegrambot.entity.Partner;
import net.dunice.mk.rsmtelegrambot.entity.Stock;
import net.dunice.mk.rsmtelegrambot.handler.MenuGenerator;
import net.dunice.mk.rsmtelegrambot.handler.state.BasicState;
import net.dunice.mk.rsmtelegrambot.handler.state.CreateStockState;
import net.dunice.mk.rsmtelegrambot.repository.PartnerRepository;
import net.dunice.mk.rsmtelegrambot.repository.StockRepository;
import net.dunice.mk.rsmtelegrambot.repository.UserRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.EnumMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CreateStockHandler implements MessageHandler {

    private static final String STOCK_INFO_TEMPLATE = """
        Заголовок: %s
        Период действия: %s — %s
        Описание: %s

        Создать акцию?
        """;

    private final Map<Long, CreateStockState> createStockStates;
    private final Map<Long, BasicState> basicStates;
    private final StockRepository stockRepository;
    private final PartnerRepository partnerRepository;
    private final UserRepository userRepository;
    private final EnumMap<Menu, ReplyKeyboard> menus;
    private final MenuGenerator menuGenerator;

    @Override
    public BasicState.BasicStep getStep() {
        return CREATE_STOCK;
    }

    @Override
    public PartialBotApiMethod<Message> handle(MessageDto messageDto, Long telegramId) {
        String text = messageDto.getText();

        CreateStockState state = createStockStates.get(telegramId);
        if (state == null) {
            state = new CreateStockState();
            createStockStates.put(telegramId, state);
            state.setPartnerId(Long.parseLong(text));
        }

        if (StringUtils.equalsAny(text, TO_MAIN_MENU, CANCEL)) {
            return goToMainMenu(telegramId);
        }

        return switch (state.getStep()) {
            case REQUEST_STOCK_IMAGE -> handleRequestStockImage(telegramId, state);
            case VALIDATE_STOCK_IMAGE -> handleValidateStockImage(messageDto, text, telegramId, state);
            case VALIDATE_STOCK_HEAD -> handleValidateStockHead(text, telegramId, state);
            case VALIDATE_STOCK_DESCRIPTION -> handleValidateStockDescription(text, telegramId, state);
            case VALIDATE_PERIOD_START -> handleValidatePeriodStart(text, telegramId, state);
            case VALIDATE_PERIOD_END -> handleValidatePeriodEnd(text, telegramId, state);
            case CONFIRM_STOCK -> handleConfirmStock(text, telegramId, state);
            case FINISH -> handleFinish(text, telegramId);
        };
    }

    private PartialBotApiMethod<Message> handleRequestStockImage(Long telegramId, CreateStockState state) {
        state.setStep(CreateStockState.StockCreationStep.VALIDATE_STOCK_IMAGE);
        return generateSendMessage(
            telegramId,
            "Отправьте изображение (логотип / фотографию) акции или нажмите 'Пропустить'.",
            menus.get(SKIP_MENU)
        );
    }

    private PartialBotApiMethod<Message> handleValidateStockImage(MessageDto messageDto,
                                                                  String text,
                                                                  Long telegramId,
                                                                  CreateStockState state) {
        if (messageDto.getImage() != null) {
            state.setImage(messageDto.getImage());
            state.setStep(CreateStockState.StockCreationStep.VALIDATE_STOCK_HEAD);
            return generateSendMessage(
                telegramId,
                "Введите заголовок акции (не более 100 символов):",
                menus.get(CANCEL_MENU)
            );
        } else if (SKIP.equalsIgnoreCase(text)) {
            state.setImage(null);
            state.setStep(CreateStockState.StockCreationStep.VALIDATE_STOCK_HEAD);
            return generateSendMessage(
                telegramId,
                "Введите заголовок акции (не более 100 символов):",
                menus.get(CANCEL_MENU)
            );
        } else {
            return generateSendMessage(
                telegramId,
                "Необходимо отправить изображение или нажать 'Пропустить'. Повторите ввод:",
                menus.get(SKIP_MENU)
            );
        }
    }

    private PartialBotApiMethod<Message> handleValidateStockHead(String text,
                                                                 Long telegramId,
                                                                 CreateStockState state) {
        if (StringUtils.isBlank(text) || text.strip().length() > 100) {
            return generateSendMessage(
                telegramId,
                "Заголовок акции не должен быть пустым и не должен превышать 100 символов. Повторите ввод:",
                menus.get(CANCEL_MENU)
            );
        }
        state.setStockHead(text.strip());

        state.setStep(CreateStockState.StockCreationStep.VALIDATE_STOCK_DESCRIPTION);
        return generateSendMessage(
            telegramId,
            "Введите описание акции (не более 250 символов):",
            menus.get(CANCEL_MENU)
        );
    }

    private PartialBotApiMethod<Message> handleValidateStockDescription(String text,
                                                                        Long telegramId,
                                                                        CreateStockState state) {
        if (StringUtils.isBlank(text) || text.strip().length() > 250) {
            return generateSendMessage(
                telegramId,
                "Описание акции не должно быть пустым и не может превышать 250 символов. Повторите ввод:",
                menus.get(CANCEL_MENU)
            );
        }
        state.setDescription(text.strip());

        state.setStep(CreateStockState.StockCreationStep.VALIDATE_PERIOD_START);
        return generateSendMessage(
            telegramId,
            "Введите дату начала акции в формате (ДД.ММ.ГГГГ):",
            menus.get(CANCEL_MENU)
        );
    }

    private PartialBotApiMethod<Message> handleValidatePeriodStart(String text,
                                                                   Long telegramId,
                                                                   CreateStockState state) {
        try {
            LocalDate startDate = LocalDate.parse(
                text.trim(),
                DateTimeFormatter.ofPattern("dd.MM.yyyy")
            );
            if (startDate.isBefore(LocalDate.now())) {
                return generateSendMessage(
                    telegramId,
                    "Дата начала акции не может быть в прошлом. Повторите ввод:",
                    menus.get(CANCEL_MENU)
                );
            }
            state.setPeriodStart(startDate);
            state.setStep(CreateStockState.StockCreationStep.VALIDATE_PERIOD_END);
            return generateSendMessage(
                telegramId,
                "Введите дату окончания акции в формате (ДД.ММ.ГГГГ):",
                menus.get(CANCEL_MENU)
            );
        } catch (DateTimeParseException e) {
            return generateSendMessage(
                telegramId,
                "Дата должна быть в формате (ДД.ММ.ГГГГ). Повторите ввод:",
                menus.get(CANCEL_MENU)
            );
        }
    }

    private PartialBotApiMethod<Message> handleValidatePeriodEnd(String text,
                                                                 Long telegramId,
                                                                 CreateStockState state) {
        try {
            LocalDate endDate = LocalDate.parse(
                text.trim(),
                DateTimeFormatter.ofPattern("dd.MM.yyyy")
            );
            if (endDate.isBefore(state.getPeriodStart())) {
                return generateSendMessage(
                    telegramId,
                    "Дата окончания не может быть раньше даты начала. Повторите ввод:",
                    menus.get(CANCEL_MENU)
                );
            }
            state.setPeriodEnd(endDate);

            state.setStep(CONFIRM_STOCK);
            return sendConfirmCreationMessage(telegramId);
        } catch (DateTimeParseException e) {
            return generateSendMessage(
                telegramId,
                "Дата должна быть в формате (ДД.ММ.ГГГГ). Повторите ввод:",
                menus.get(CANCEL_MENU)
            );
        }
    }

    private PartialBotApiMethod<Message> handleConfirmStock(String text,
                                                            Long telegramId,
                                                            CreateStockState state) {
        if (YES.equalsIgnoreCase(text)) {
            saveStock(state);
            state.setStep(CreateStockState.StockCreationStep.FINISH);
            return generateSendMessage(
                telegramId,
                "Акция успешно создана!",
                menus.get(GO_TO_MAIN_MENU)
            );
        } else if (NO.equalsIgnoreCase(text)) {
            state.setStep(CreateStockState.StockCreationStep.FINISH);
            return generateSendMessage(
                telegramId,
                "Создание акции отменено.",
                menus.get(GO_TO_MAIN_MENU)
            );
        } else {
            return sendConfirmCreationMessage(telegramId);
        }
    }

    private PartialBotApiMethod<Message> handleFinish(String text, Long telegramId) {
        if (TO_MAIN_MENU.equalsIgnoreCase(text)) {
            return goToMainMenu(telegramId);
        }
        return generateSendMessage(
            telegramId,
            "Неверная команда",
            menus.get(GO_TO_MAIN_MENU)
        );
    }

    private void saveStock(CreateStockState state) {
        Partner partner = partnerRepository.findById(state.getPartnerId()).get();

        Stock stock = new Stock();
        stock.setPartnerTelegramId(partner);
        stock.setImage(state.getImage());
        stock.setHead(state.getStockHead());
        stock.setDescription(state.getDescription());
        stock.setPeriodStocksStart(state.getPeriodStart());
        stock.setPeriodStocksEnd(state.getPeriodEnd());

        stockRepository.save(stock);
    }

    private SendMessage goToMainMenu(Long telegramId) {
        createStockStates.remove(telegramId);

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

    private PartialBotApiMethod<Message> sendConfirmCreationMessage(Long telegramId) {
        CreateStockState state = createStockStates.get(telegramId);

        String message = STOCK_INFO_TEMPLATE.formatted(
            state.getStockHead(),
            state.getPeriodStart().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
            state.getPeriodEnd().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
            state.getDescription()
        );

        if (isImagePresent(state.getImage())) {
            SendPhoto sendPhoto = generateImageMessage(telegramId, message, menus.get(SELECTION_MENU), state.getImage());
            sendPhoto.setParseMode("HTML");
            return sendPhoto;
        } else {
            SendMessage sendMessage = generateSendMessage(telegramId, message, menus.get(SELECTION_MENU));
            sendMessage.setParseMode("HTML");
            return sendMessage;
        }
    }

    private boolean isImagePresent(byte[] image) {
        return image != null && image.length > 0;
    }
}
