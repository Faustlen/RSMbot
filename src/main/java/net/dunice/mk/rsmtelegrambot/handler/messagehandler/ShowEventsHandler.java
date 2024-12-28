package net.dunice.mk.rsmtelegrambot.handler.messagehandler;

import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.CANCEL;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.DELETE;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.EDIT_EVENT;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.EVENTS_LIST;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.NO;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.TO_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.YES;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.CANCEL_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.EVENT_FIELDS_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.GO_TO_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.SELECTION_MENU;
import static net.dunice.mk.rsmtelegrambot.entity.Role.USER;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.IN_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.SHOW_EVENTS;
import static net.dunice.mk.rsmtelegrambot.handler.state.stateobject.ShowEventsState.ShowEventsStep.CONFIRM_EVENT_EDIT;
import static net.dunice.mk.rsmtelegrambot.handler.state.stateobject.ShowEventsState.ShowEventsStep.EDIT_EVENT_FIELD;
import static net.dunice.mk.rsmtelegrambot.handler.state.stateobject.ShowEventsState.ShowEventsStep.HANDLE_USER_ACTION;
import static net.dunice.mk.rsmtelegrambot.handler.state.stateobject.ShowEventsState.ShowEventsStep.SELECT_EVENT_FIELD;
import static net.dunice.mk.rsmtelegrambot.handler.state.stateobject.ShowEventsState.ShowEventsStep.SHOW_EVENTS_LIST;
import static net.dunice.mk.rsmtelegrambot.handler.state.stateobject.ShowEventsState.ShowEventsStep.SHOW_EVENT_DETAILS;

import lombok.RequiredArgsConstructor;
import net.dunice.mk.rsmtelegrambot.constant.Menu;
import net.dunice.mk.rsmtelegrambot.dto.MessageDto;
import net.dunice.mk.rsmtelegrambot.entity.Event;
import net.dunice.mk.rsmtelegrambot.entity.User;
import net.dunice.mk.rsmtelegrambot.handler.MenuGenerator;
import net.dunice.mk.rsmtelegrambot.handler.state.BasicState;
import net.dunice.mk.rsmtelegrambot.handler.state.stateobject.ShowEventsState;
import net.dunice.mk.rsmtelegrambot.repository.EventRepository;
import net.dunice.mk.rsmtelegrambot.repository.UserRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ShowEventsHandler implements MessageHandler {

    private final EventRepository eventRepository;
    private final MenuGenerator menuGenerator;
    private final Map<Long, BasicState> basicStates;
    private final UserRepository userRepository;
    private final Map<Long, ShowEventsState> showEventStates;
    private final Map<Menu, ReplyKeyboard> menus;
    private static final String EVENT_INFO_TEMPLATE = """
        Мероприятие: %s
        Описание: %s
        Ссылка: %s
        Дата: %s  |  Время: %s
        """;

    @Override
    public BasicState getState() {
        return SHOW_EVENTS;
    }

    @Override
    public SendMessage handle(MessageDto messageDto, Long telegramId) {
        String text = messageDto.getText();
        ShowEventsState state = showEventStates.get(telegramId);
        if (state == null) {
            showEventStates.put(telegramId, (state = new ShowEventsState()));
        }

        return switch (state.getStep()) {
            case SHOW_EVENTS_LIST -> {
                List<Event> events = eventRepository.findAll();
                state.setStep(SHOW_EVENT_DETAILS);
                yield generateSendMessage(telegramId, "Выберите интересующее вас мероприятие: ",
                    generateEventListKeyboard(events));
            }
            case SHOW_EVENT_DETAILS -> {
                if (TO_MAIN_MENU.equalsIgnoreCase(text)) {
                    yield goToMainMenu(telegramId);
                }
                try {
                    Integer eventId = Integer.valueOf(text.substring(text.lastIndexOf(' ') + 1));
                    Optional<Event> eventOptional = eventRepository.findById(eventId);
                    if (eventOptional.isPresent()) {
                        Optional<User> userOptional = userRepository.findById(telegramId);
                        Event targetEvent = eventOptional.get();
                        state.setTargetEvent(targetEvent);
                        String eventDescription = getEventDescription(targetEvent);
                        state.setStep(HANDLE_USER_ACTION);
                        yield generateSendMessage(telegramId, eventDescription, getUserActionKeyboard(userOptional));
                    } else {
                        yield generateSendMessage(telegramId, "Мероприятие не найдено.");
                    }
                } catch (NumberFormatException e) {
                    yield generateSendMessage(telegramId, "Неверный ID мероприятия.");
                }
            }
            case HANDLE_USER_ACTION -> {
                if (TO_MAIN_MENU.equalsIgnoreCase(text)) {
                    yield goToMainMenu(telegramId);
                } else if (EVENTS_LIST.equalsIgnoreCase(text)) {
                    state.setStep(SHOW_EVENTS_LIST);
                    yield handle(messageDto, telegramId);
                } else if (text.equals(DELETE)) {
                    eventRepository.delete(state.getTargetEvent());
                    state.setStep(SHOW_EVENTS_LIST);
                    yield handle(messageDto, telegramId);
                } else if (text.equals(EDIT_EVENT)) {
                    state.setStep(SELECT_EVENT_FIELD);
                    yield generateSendMessage(telegramId, "Выберите поле, которое хотите отредактировать:",
                        menus.get(EVENT_FIELDS_MENU));
                } else {
                    yield generateSendMessage(telegramId, "Неверная команда", menus.get(GO_TO_MAIN_MENU));
                }
            }

            case SELECT_EVENT_FIELD -> {
                if (text.equalsIgnoreCase("Название")) {
                    state.setStep(EDIT_EVENT_FIELD);
                    state.setEditingFieldName(text);
                    yield generateSendMessage(telegramId, "Введите новое название мероприятия:",
                        menus.get(CANCEL_MENU));
                } else if (text.equalsIgnoreCase("Описание")) {
                    state.setStep(EDIT_EVENT_FIELD);
                    state.setEditingFieldName(text);
                    yield generateSendMessage(telegramId, "Введите новое описание мероприятиям",
                        menus.get(CANCEL_MENU));
                } else if (text.equalsIgnoreCase("Дата и Время")) {
                    state.setStep(EDIT_EVENT_FIELD);
                    state.setEditingFieldName(text);
                    yield generateSendMessage(telegramId,
                        "Введите новую дату мероприятия в формате (ДД.ММ.ГГГГ-ЧЧ:ММ) :", menus.get(CANCEL_MENU));
                } else if (text.equalsIgnoreCase("Ссылка")) {
                    state.setStep(EDIT_EVENT_FIELD);
                    state.setEditingFieldName(text);
                    yield generateSendMessage(telegramId, "Введите новую ссылку:", menus.get(CANCEL_MENU));
                } else {
                    yield generateSendMessage(telegramId, "Неверное поле. Выберите одно из доступных.",
                        menus.get(EVENT_FIELDS_MENU));
                }
            }

            case EDIT_EVENT_FIELD -> {
                if (CANCEL.equalsIgnoreCase(text)) {
                    state.setStep(SHOW_EVENT_DETAILS);
                    messageDto.setText(" " + state.getTargetEvent().getEventId());
                    yield handle(messageDto, telegramId);
                }
                try {
                    Event targetEvent = state.getTargetEvent();
                    switch (state.getEditingFieldName()) {
                        case "Название" -> targetEvent.setTitle(text.trim());
                        case "Описание" -> targetEvent.setText(text.trim());
                        case "Дата и Время" -> targetEvent.setEventDate(
                            LocalDateTime.parse(text.trim(), DateTimeFormatter.ofPattern("dd.MM.yyyy-HH:mm")));
                        case "Ссылка" -> targetEvent.setLink(text.trim());
                    }
                    state.setStep(CONFIRM_EVENT_EDIT);
                    yield generateSendMessage(telegramId,
                        "Мероприятие с новыми данными:\n" + getEventDescription(targetEvent) + "\nСохранить изменения?",
                        menus.get(SELECTION_MENU));
                } catch (DateTimeParseException e) {
                    yield generateSendMessage(telegramId,
                        "Дата должна быть в формате (ДД.ММ.ГГГГ-ЧЧ:ММ). Повторите ввод:");
                } catch (Exception e) {
                    yield generateSendMessage(telegramId, "Ошибка при редактировании поля. Повторите ввод:");
                }
            }

            case CONFIRM_EVENT_EDIT -> {
                if (StringUtils.equalsAny(text, YES, NO)) {
                    if (YES.equalsIgnoreCase(text)) {
                        eventRepository.save(state.getTargetEvent());
                    }
                    state.setStep(SHOW_EVENT_DETAILS);
                    messageDto.setText(" " + state.getTargetEvent().getEventId());
                    yield handle(messageDto, telegramId);
                }
                else {
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

    private String getEventDescription(Event targetEvent) {
        return EVENT_INFO_TEMPLATE.formatted(
            targetEvent.getTitle(),
            targetEvent.getText(),
            targetEvent.getLink(),
            targetEvent.getEventDate().toLocalDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
            targetEvent.getEventDate().toLocalTime().format(DateTimeFormatter.ofPattern("hh:mm")));
    }

    private ReplyKeyboardMarkup generateEventListKeyboard(List<Event> events) {
        ReplyKeyboardMarkup replyMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow firstRow = new KeyboardRow();
        firstRow.add(TO_MAIN_MENU);
        keyboard.add(firstRow);
        for (int i = 0; i < events.size(); ) {
            KeyboardRow row = new KeyboardRow();
            Event event = events.get(i++);
            row.add("%s | ID: %s".formatted(event.getTitle(), event.getEventId()));
            keyboard.add(row);
        }
        replyMarkup.setKeyboard(keyboard);
        replyMarkup.setResizeKeyboard(true);
        replyMarkup.setOneTimeKeyboard(false);
        return replyMarkup;
    }

    private ReplyKeyboard getUserActionKeyboard(Optional<User> userOptional) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        InlineKeyboardButton toMainMenuButton = new InlineKeyboardButton(TO_MAIN_MENU);
        toMainMenuButton.setCallbackData(toMainMenuButton.getText());
        InlineKeyboardButton toEventsButton = new InlineKeyboardButton(EVENTS_LIST);
        toEventsButton.setCallbackData(toEventsButton.getText());
        keyboard.add(List.of(toMainMenuButton));
        keyboard.add(List.of(toEventsButton));
        if (userOptional.isPresent() && userOptional.get().getUserRole() != USER) {
            InlineKeyboardButton deleteEventButton = new InlineKeyboardButton(DELETE);
            deleteEventButton.setCallbackData(deleteEventButton.getText());
            keyboard.add(List.of(deleteEventButton));
            InlineKeyboardButton editEventButton = new InlineKeyboardButton(EDIT_EVENT);
            editEventButton.setCallbackData(editEventButton.getText());
            keyboard.add(List.of(editEventButton));
        }
        inlineKeyboardMarkup.setKeyboard(keyboard);
        return inlineKeyboardMarkup;
    }

    private SendMessage goToMainMenu(Long telegramId) {
        showEventStates.remove(telegramId);
        basicStates.put(telegramId, IN_MAIN_MENU);
        return menuGenerator.generateRoleSpecificMainMenu(telegramId,
            userRepository.findByTelegramId(telegramId).get().getUserRole());
    }
}