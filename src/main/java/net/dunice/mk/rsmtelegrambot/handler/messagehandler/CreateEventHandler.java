package net.dunice.mk.rsmtelegrambot.handler.messagehandler;

import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.CANCEL;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.NO;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.TO_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.YES;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.CANCEL_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.GO_TO_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.SELECTION_MENU;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.BasicStep;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.BasicStep.CREATE_EVENT;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.BasicStep.IN_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.handler.state.EventCreationState.EventCreationStep.CONFIRM_EVENT;
import static net.dunice.mk.rsmtelegrambot.handler.state.EventCreationState.EventCreationStep.FINISH;
import static net.dunice.mk.rsmtelegrambot.handler.state.EventCreationState.EventCreationStep.VALIDATE_EVENT_ADDRESS;
import static net.dunice.mk.rsmtelegrambot.handler.state.EventCreationState.EventCreationStep.VALIDATE_EVENT_DATE_TIME;
import static net.dunice.mk.rsmtelegrambot.handler.state.EventCreationState.EventCreationStep.VALIDATE_EVENT_DESCRIPTION;
import static net.dunice.mk.rsmtelegrambot.handler.state.EventCreationState.EventCreationStep.VALIDATE_EVENT_LINK;
import static net.dunice.mk.rsmtelegrambot.handler.state.EventCreationState.EventCreationStep.VALIDATE_EVENT_NAME;

import lombok.RequiredArgsConstructor;
import net.dunice.mk.rsmtelegrambot.constant.Menu;
import net.dunice.mk.rsmtelegrambot.dto.MessageDto;
import net.dunice.mk.rsmtelegrambot.entity.Event;
import net.dunice.mk.rsmtelegrambot.handler.MenuGenerator;
import net.dunice.mk.rsmtelegrambot.handler.state.BasicState;
import net.dunice.mk.rsmtelegrambot.handler.state.EventCreationState;
import net.dunice.mk.rsmtelegrambot.repository.EventRepository;
import net.dunice.mk.rsmtelegrambot.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
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
        Адресс: %s
        """;
    private final Map<Long, EventCreationState> eventCreationStates;
    private final Map<Long, BasicState> basicStates;
    private final EventRepository eventRepository;
    private final EnumMap<Menu, ReplyKeyboard> menus;
    private final MenuGenerator menuGenerator;
    private final UserRepository userRepository;

    @Override
    public SendMessage handle(MessageDto messageDto, Long telegramId) {
        String text = messageDto.getText();
        EventCreationState state = eventCreationStates.get(telegramId);
        if (state == null) {
            eventCreationStates.put(telegramId, (state = new EventCreationState()));
        }

        if (TO_MAIN_MENU.equals(text)) {
            return goToMainMenu(telegramId);
        }

        return switch (state.getStep()) {
            case REQUEST_EVENT_NAME -> {
                state.setStep(VALIDATE_EVENT_NAME);
                yield generateSendMessage(telegramId, "Введите название мероприятия (не более 100 символов):",
                    menus.get(CANCEL_MENU));
            }
            case VALIDATE_EVENT_NAME -> {
                if (CANCEL.equalsIgnoreCase(text)) {
                    yield goToMainMenu(telegramId);
                } else if (text.strip().length() > 100) {
                    yield generateSendMessage(telegramId,
                        "Название мероприятия не должно превышать 100 символов. Повторите ввод:",
                        menus.get(CANCEL_MENU));
                }
                state.setEventName(text.strip());
                state.setStep(VALIDATE_EVENT_DESCRIPTION);
                yield generateSendMessage(telegramId, "Введите описание мероприятия (не более 250 символов):",
                    menus.get(CANCEL_MENU));
            }
            case VALIDATE_EVENT_DESCRIPTION -> {
                if (CANCEL.equalsIgnoreCase(text)) {
                    yield goToMainMenu(telegramId);
                } else if (text.strip().length() > 250) {
                    yield generateSendMessage(telegramId,
                        "Описание мероприятия не должно превышать 250 символов. Повторите ввод:",
                        menus.get(CANCEL_MENU));
                }
                state.setEventDescription(text.strip());
                state.setStep(VALIDATE_EVENT_DATE_TIME);
                yield generateSendMessage(telegramId,
                    "Введите дату и время проведения мероприятия в следующем формате (ДД.ММ.ГГГГ-ЧЧ:ММ)",
                    menus.get(CANCEL_MENU));
            }
            case VALIDATE_EVENT_DATE_TIME -> {
                try {
                    if (CANCEL.equalsIgnoreCase(text)) {
                        yield goToMainMenu(telegramId);
                    } else {
                        LocalDateTime eventDateTime =
                            LocalDateTime.parse(text.trim(), DateTimeFormatter.ofPattern("dd.MM.yyyy-HH:mm"));
                        if (eventDateTime.isBefore(LocalDateTime.now())) {
                            yield generateSendMessage(telegramId,
                                "Дата и время мероприятия не могут быть в прошлом. Повторите ввод:",
                                menus.get(CANCEL_MENU));
                        }
                        state.setEventDateTime(eventDateTime);
                        state.setStep(VALIDATE_EVENT_LINK);
                        yield generateSendMessage(telegramId, "Введите ссылку на мероприятие (не более 250 символов):",
                            menus.get(CANCEL_MENU));
                    }
                } catch (DateTimeParseException e) {
                    yield generateSendMessage(telegramId,
                        "Дата и время должны быть в формате (ДД.ММ.ГГГГ-ЧЧ:ММ). Повторите ввод:",
                        menus.get(CANCEL_MENU));
                }
            }
            case VALIDATE_EVENT_LINK -> {
                if (CANCEL.equalsIgnoreCase(text)) {
                    yield goToMainMenu(telegramId);
                } else if (text.strip().length() > 250) {
                    yield generateSendMessage(telegramId, "Ссылка не должна превышать 250 символов. Повторите ввод:",
                        menus.get(CANCEL_MENU));
                }
                state.setEventLink(text.strip());
                state.setStep(VALIDATE_EVENT_ADDRESS);
                yield generateSendMessage(telegramId,
                    "Введите адрес (улица и номер дома, например Крестьянская 207): ",
                    menus.get(CANCEL_MENU));
            }
            case VALIDATE_EVENT_ADDRESS -> {
                if (text != null && text.length() <= 255) {
                    String address = text.trim();
                    String[] parts = address.split(" ");
                    if (parts.length != 2) {
                        yield generateSendMessage(telegramId,
                            "Адрес должен соответствовать формату (улица и номер дома, например Крестьянская 207), Повторите ввод (до 255 символов):",
                            menus.get(CANCEL_MENU));
                    }
                    address = "г. Майкоп, ул. %s, д. %s".formatted(parts[0], parts[1]);
                    state.setAddress(address);
                    state.setStep(CONFIRM_EVENT);
                    yield generateSendMessage(telegramId, "Создать мероприятие?\n" +
                            EVENT_INFO_TEMPLATE.formatted(
                                state.getEventName(),
                                state.getEventDescription(),
                                state.getEventDateTime()
                                    .format(
                                        DateTimeFormatter.ofPattern("dd.MM.yyyy | HH:mm")),
                                state.getEventLink(),
                                state.getAddress()),
                        menus.get(Menu.SELECTION_MENU));
                } else {
                    yield generateSendMessage(telegramId,
                        "Адрес слишком длинный. Повторите ввод (до 255 символов):", menus.get(CANCEL_MENU));
                }
            }
            case CONFIRM_EVENT -> {
                if (YES.equalsIgnoreCase(text)) {
                    saveEvent(state);
                    state.setStep(FINISH);
                    yield generateSendMessage(telegramId, "Мероприятие успешно создано!", menus.get(GO_TO_MAIN_MENU));
                } else if (NO.equalsIgnoreCase(text)) {
                    state.setStep(FINISH);
                    yield generateSendMessage(telegramId, "Создание мероприятия отменено.", menus.get(GO_TO_MAIN_MENU));
                } else {
                    yield generateSendMessage(telegramId, "Неверная команда.", menus.get(SELECTION_MENU));
                }
            }
            case FINISH -> {
                if (TO_MAIN_MENU.equalsIgnoreCase(text)) {
                    yield goToMainMenu(telegramId);
                } else {
                    yield generateSendMessage(telegramId, "Неверная команда", menus.get(GO_TO_MAIN_MENU));
                }
            }
        };
    }

    private void saveEvent(EventCreationState state) {
        Event event = new Event();
        event.setTitle(state.getEventName());
        event.setText(state.getEventDescription());
        event.setEventDate(state.getEventDateTime());
        event.setAddress(state.getAddress());
        event.setLink(state.getEventLink());
        eventRepository.save(event);
    }

    @Override
    public BasicStep getStep() {
        return CREATE_EVENT;
    }

    private SendMessage goToMainMenu(Long telegramId) {
        BasicState state = basicStates.get(telegramId);
        eventCreationStates.remove(telegramId);
        state.setStep(IN_MAIN_MENU);
        return menuGenerator.generateRoleSpecificMainMenu(telegramId,
            state.getUser().getUserRole());
    }
}
