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
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.BasicStep;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.BasicStep.CREATE_EVENT;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.BasicStep.IN_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.handler.state.EventCreationState.EventCreationStep.CONFIRM_EVENT;


import lombok.RequiredArgsConstructor;
import net.dunice.mk.rsmtelegrambot.constant.Menu;
import net.dunice.mk.rsmtelegrambot.dto.MessageDto;
import net.dunice.mk.rsmtelegrambot.entity.Event;
import net.dunice.mk.rsmtelegrambot.handler.MenuGenerator;
import net.dunice.mk.rsmtelegrambot.handler.state.BasicState;
import net.dunice.mk.rsmtelegrambot.handler.state.EventCreationState;
import net.dunice.mk.rsmtelegrambot.repository.EventRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.EnumMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CreateEventHandler implements MessageHandler {
    private static final String EVENT_INFO_TEMPLATE = """
        Название: %s
        Описание: %s
        Дата и время: %s
        Ссылка: %s
        Адрес: %s
        
        Создать мероприятие?
        """;

    private final Map<Long, EventCreationState> eventCreationStates;
    private final Map<Long, BasicState> basicStates;
    private final EventRepository eventRepository;
    private final EnumMap<Menu, ReplyKeyboard> menus;
    private final MenuGenerator menuGenerator;

    @Override
    public BasicStep getStep() {
        return CREATE_EVENT;
    }

    @Override
    public PartialBotApiMethod<Message> handle(MessageDto messageDto, Long telegramId) {
        String text = messageDto.getText();

        EventCreationState state = eventCreationStates.get(telegramId);
        if (state == null) {
            state = new EventCreationState();
            eventCreationStates.put(telegramId, state);
        }

        if (StringUtils.equalsAny(text, TO_MAIN_MENU, CANCEL)) {
            return goToMainMenu(telegramId);
        }

        return switch (state.getStep()) {
            case REQUEST_EVENT_LOGO -> handleRequestEventLogo(telegramId, state);
            case VALIDATE_EVENT_LOGO -> handleValidateEventLogo(messageDto, text, telegramId, state);
            case VALIDATE_EVENT_NAME -> handleValidateEventName(text, telegramId, state);
            case VALIDATE_EVENT_DESCRIPTION -> handleValidateEventDescription(text, telegramId, state);
            case VALIDATE_EVENT_DATE_TIME -> handleValidateEventDateTime(text, telegramId, state);
            case VALIDATE_EVENT_LINK -> handleValidateEventLink(text, telegramId, state);
            case VALIDATE_EVENT_ADDRESS -> handleValidateEventAddress(text, telegramId, state);
            case CONFIRM_EVENT -> handleConfirmEvent(text, telegramId, state);
            case FINISH -> handleFinish(text, telegramId);
        };
    }

    private PartialBotApiMethod<Message> handleRequestEventLogo(Long telegramId, EventCreationState state) {
        state.setStep(EventCreationState.EventCreationStep.VALIDATE_EVENT_LOGO);
        return generateSendMessage(
            telegramId,
            "Отправьте логотип мероприятия (изображение) или нажмите 'Пропустить'.",
            menus.get(SKIP_MENU)
        );
    }

    private PartialBotApiMethod<Message> handleValidateEventLogo(MessageDto messageDto,
                                                                 String text,
                                                                 Long telegramId,
                                                                 EventCreationState state) {
        if (messageDto.getImage() != null) {
            state.setLogo(messageDto.getImage());
            state.setStep(EventCreationState.EventCreationStep.VALIDATE_EVENT_NAME);
            return generateSendMessage(
                telegramId,
                "Введите название мероприятия:",
                menus.get(CANCEL_MENU)
            );
        } else if (SKIP.equalsIgnoreCase(text)) {
            state.setLogo(null);
            state.setStep(EventCreationState.EventCreationStep.VALIDATE_EVENT_NAME);
            return generateSendMessage(
                telegramId,
                "Введите название мероприятия:",
                menus.get(CANCEL_MENU)
            );
        } else {
            return generateSendMessage(
                telegramId,
                "Логотип должен быть изображением. Повторите ввод:",
                menus.get(SKIP_MENU)
            );
        }
    }

    private PartialBotApiMethod<Message> handleValidateEventName(String text,
                                                                 Long telegramId,
                                                                 EventCreationState state) {

        if (text == null || text.strip().length() > 100) {
            return generateSendMessage(
                telegramId,
                "Название мероприятия не должно превышать 100 символов или быть пустым. Повторите ввод:",
                menus.get(CANCEL_MENU)
            );
        }

        state.setEventName(text.strip());
        state.setStep(EventCreationState.EventCreationStep.VALIDATE_EVENT_DESCRIPTION);
        return generateSendMessage(
            telegramId,
            "Введите описание мероприятия (не более 250 символов):",
            menus.get(CANCEL_MENU)
        );
    }

    private PartialBotApiMethod<Message> handleValidateEventDescription(String text,
                                                                        Long telegramId,
                                                                        EventCreationState state) {

        if (text == null || text.strip().length() > 250) {
            return generateSendMessage(
                telegramId,
                "Описание мероприятия не должно превышать 250 символов или быть пустым. Повторите ввод:",
                menus.get(CANCEL_MENU)
            );
        }

        state.setEventDescription(text.strip());
        state.setStep(EventCreationState.EventCreationStep.VALIDATE_EVENT_DATE_TIME);
        return generateSendMessage(
            telegramId,
            "Введите дату и время проведения мероприятия в следующем формате (ДД.ММ.ГГГГ-ЧЧ:ММ)",
            menus.get(CANCEL_MENU)
        );
    }

    private PartialBotApiMethod<Message> handleValidateEventDateTime(String text,
                                                                     Long telegramId,
                                                                     EventCreationState state) {

        try {
            LocalDateTime eventDateTime = LocalDateTime.parse(
                text.trim(),
                DateTimeFormatter.ofPattern("dd.MM.yyyy-HH:mm")
            );
            if (eventDateTime.isBefore(LocalDateTime.now())) {
                return generateSendMessage(
                    telegramId,
                    "Дата и время мероприятия не могут быть в прошлом. Повторите ввод:",
                    menus.get(CANCEL_MENU)
                );
            }
            state.setEventDateTime(eventDateTime);
            state.setStep(EventCreationState.EventCreationStep.VALIDATE_EVENT_LINK);

            return generateSendMessage(
                telegramId,
                "Введите ссылку на мероприятие (не более 250 символов):",
                menus.get(CANCEL_MENU)
            );
        } catch (DateTimeParseException | NullPointerException e) {
            return generateSendMessage(
                telegramId,
                "Дата и время должны быть в формате (ДД.ММ.ГГГГ-ЧЧ:ММ). Повторите ввод:",
                menus.get(CANCEL_MENU)
            );
        }
    }

    private PartialBotApiMethod<Message> handleValidateEventLink(String text,
                                                                 Long telegramId,
                                                                 EventCreationState state) {

        if (text == null || text.strip().length() > 250) {
            return generateSendMessage(
                telegramId,
                "Ссылка не должна превышать 250 символов или быть пустой. Повторите ввод:",
                menus.get(CANCEL_MENU)
            );
        }

        state.setEventLink(text.strip());
        state.setStep(EventCreationState.EventCreationStep.VALIDATE_EVENT_ADDRESS);
        return generateSendMessage(
            telegramId,
            "Введите адрес (улица и номер дома, например Крестьянская 207):",
            menus.get(CANCEL_MENU)
        );
    }

    private PartialBotApiMethod<Message> handleValidateEventAddress(String text,
                                                                    Long telegramId,
                                                                    EventCreationState state) {
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

        state.setStep(CONFIRM_EVENT);
        return sendConfirmCreationMessage(telegramId);
    }

    private PartialBotApiMethod<Message> handleConfirmEvent(String text,
                                                            Long telegramId,
                                                            EventCreationState state) {
        if (YES.equalsIgnoreCase(text)) {
            saveEvent(state);
            state.setStep(EventCreationState.EventCreationStep.FINISH);
            return generateSendMessage(
                telegramId,
                "Мероприятие успешно создано!",
                menus.get(GO_TO_MAIN_MENU)
            );
        } else if (NO.equalsIgnoreCase(text)) {
            state.setStep(EventCreationState.EventCreationStep.FINISH);
            return generateSendMessage(
                telegramId,
                "Создание мероприятия отменено.",
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

    private void saveEvent(EventCreationState state) {
        Event event = new Event();
        event.setTitle(state.getEventName());
        event.setText(state.getEventDescription());
        event.setEventDate(state.getEventDateTime());
        event.setAddress(state.getAddress());
        event.setLink(state.getEventLink());
        event.setLogo(state.getLogo());
        eventRepository.save(event);
    }

    private SendMessage goToMainMenu(Long telegramId) {
        BasicState baseState = basicStates.get(telegramId);
        eventCreationStates.remove(telegramId);
        baseState.setStep(IN_MAIN_MENU);
        return menuGenerator.generateRoleSpecificMainMenu(
            telegramId,
            baseState.getUser().getUserRole()
        );
    }

    private PartialBotApiMethod<Message> sendConfirmCreationMessage(Long telegramId) {
        EventCreationState state = eventCreationStates.get(telegramId);

        String message = EVENT_INFO_TEMPLATE.formatted(
            state.getEventName(),
            state.getEventDescription(),
            state.getEventDateTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy | HH:mm")),
            state.getEventLink(),
            state.getAddress()
        );

        if (isLogoPresent(state.getLogo())) {
            SendPhoto sendPhoto = generateImageMessage(telegramId, message, menus.get(SELECTION_MENU), state.getLogo());
            sendPhoto.setParseMode("HTML");
            return sendPhoto;
        } else {
            SendMessage sendMessage = generateSendMessage(telegramId, message, menus.get(SELECTION_MENU));
            sendMessage.setParseMode("HTML");
            return sendMessage;
        }
    }

    private boolean isLogoPresent(byte[] logo) {
        return logo != null && logo.length > 0;
    }
}
