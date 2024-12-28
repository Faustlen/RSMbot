package net.dunice.mk.rsmtelegrambot.handler.messagehandler;

import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.TO_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.GO_TO_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.CREATE_EVENT;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.IN_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.handler.state.stateobject.EventCreationState.EventCreationStep.CONFIRM_EVENT;
import static net.dunice.mk.rsmtelegrambot.handler.state.stateobject.EventCreationState.EventCreationStep.VERIFY_EVENT_DATE_TIME;
import static net.dunice.mk.rsmtelegrambot.handler.state.stateobject.EventCreationState.EventCreationStep.VERIFY_EVENT_DESCRIPTION;
import static net.dunice.mk.rsmtelegrambot.handler.state.stateobject.EventCreationState.EventCreationStep.VERIFY_EVENT_LINK;
import static net.dunice.mk.rsmtelegrambot.handler.state.stateobject.EventCreationState.EventCreationStep.VERIFY_EVENT_NAME;

import lombok.RequiredArgsConstructor;
import net.dunice.mk.rsmtelegrambot.constant.Menu;
import net.dunice.mk.rsmtelegrambot.dto.MessageDto;
import net.dunice.mk.rsmtelegrambot.entity.Event;
import net.dunice.mk.rsmtelegrambot.handler.MenuGenerator;
import net.dunice.mk.rsmtelegrambot.handler.state.BasicState;
import net.dunice.mk.rsmtelegrambot.handler.state.stateobject.EventCreationState;
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
    private final Map<Long, EventCreationState> eventCreationStates;
    private final Map<Long, BasicState> basicStates;
    private final EventRepository eventRepository;
    private final EnumMap<Menu, ReplyKeyboard> menus;
    private final MenuGenerator menuGenerator;
    private final UserRepository userRepository;

    private static final String EVENT_INFO_TEMPLATE = """
        Название: %s
        Описание: %s
        Дата и время: %s
        Ссылка: %s
        """;

    @Override
    public SendMessage handle(MessageDto messageDto, Long telegramId) {
        String text = messageDto.getText();
        EventCreationState state = eventCreationStates.get(telegramId);
        if (state == null) {
            eventCreationStates.put(telegramId, (state = new EventCreationState()));
        }
        ReplyKeyboard mainMenu = menus.get(GO_TO_MAIN_MENU);

        if (TO_MAIN_MENU.equals(text)) {
            return goToMainMenu(telegramId);
        }

        return switch (state.getStep()) {
            case REQUEST_EVENT_NAME -> {
                state.setStep(VERIFY_EVENT_NAME);
                yield generateSendMessage(telegramId, "Введите название мероприятия (не более 100 символов):");
            }
            case VERIFY_EVENT_NAME -> {
                if (text.strip().length() > 100) {
                    yield generateSendMessage(telegramId,
                        "Название мероприятия не должно превышать 100 символов. Повторите ввод:");
                }
                state.setEventName(text.strip());
                state.setStep(VERIFY_EVENT_DESCRIPTION);
                yield generateSendMessage(telegramId, "Введите описание мероприятия (не более 250 символов):");
            }
            case VERIFY_EVENT_DESCRIPTION -> {
                if (text.strip().length() > 250) {
                    yield generateSendMessage(telegramId,
                        "Описание мероприятия не должно превышать 250 символов. Повторите ввод:");
                }
                state.setEventDescription(text.strip());
                state.setStep(VERIFY_EVENT_DATE_TIME);
                yield generateSendMessage(telegramId,
                    "Введите дату и время проведения мероприятия в следующем формате (ДД.ММ.ГГГГ-ЧЧ:ММ)");
            }
            case VERIFY_EVENT_DATE_TIME -> {
                try {
                    LocalDateTime eventDateTime =
                        LocalDateTime.parse(text.trim(), DateTimeFormatter.ofPattern("dd.MM.yyyy-HH:mm"));
                    if (eventDateTime.isBefore(LocalDateTime.now())) {
                        yield generateSendMessage(telegramId,
                            "Дата и время мероприятия не могут быть в прошлом. Повторите ввод:");
                    }
                    state.setEventDateTime(eventDateTime);
                    state.setStep(VERIFY_EVENT_LINK);
                    yield generateSendMessage(telegramId, "Введите ссылку на мероприятие (не более 250 символов):");
                } catch (DateTimeParseException e) {
                    yield generateSendMessage(telegramId,
                        "Дата и время должны быть в формате (ДД.ММ.ГГГГ-ЧЧ:ММ). Повторите ввод:");
                }
            }
            case VERIFY_EVENT_LINK -> {
                if (text.strip().length() > 250) {
                    yield generateSendMessage(telegramId, "Ссылка не должна превышать 250 символов. Повторите ввод:");
                }
                state.setEventLink(text.strip());
                state.setStep(CONFIRM_EVENT);
                yield generateSendMessage(telegramId, "Создать мероприятие?\n" +
                                                      EVENT_INFO_TEMPLATE.formatted(
                                                          state.getEventName(),
                                                          state.getEventDescription(),
                                                          state.getEventDateTime()
                                                              .format(DateTimeFormatter.ofPattern("dd.MM.yyyy | HH:mm")),
                                                          state.getEventLink()), menus.get(Menu.SELECTION_MENU));
            }
            case CONFIRM_EVENT -> {
                if ("Да".equalsIgnoreCase(text.strip())) {
                    saveEvent(state);
                    yield generateSendMessage(telegramId, "Мероприятие успешно создано!", mainMenu);
                } else if ("Нет".equalsIgnoreCase(text.strip())) {
                    yield generateSendMessage(telegramId, "Создание мероприятия отменено.", mainMenu);
                } else {
                    yield generateSendMessage(telegramId, "Неверная команда, регистрация отменена", mainMenu);
                }
            }
            default -> generateSendMessage(telegramId, "Произошла ошибка. Попробуйте снова.", mainMenu);
        };
    }

    private void saveEvent(EventCreationState state) {
        Event event = new Event();
        event.setTitle(state.getEventName());
        event.setText(state.getEventDescription());
        event.setEventDate(state.getEventDateTime());
        event.setLink(state.getEventLink());
        eventRepository.save(event);
    }

    @Override
    public BasicState getState() {
        return CREATE_EVENT;
    }

    private SendMessage goToMainMenu(Long telegramId) {
        eventCreationStates.remove(telegramId);
        basicStates.put(telegramId, IN_MAIN_MENU);
        return menuGenerator.generateRoleSpecificMainMenu(telegramId,
            userRepository.findByTelegramId(telegramId).get().getUserRole());
    }
}
