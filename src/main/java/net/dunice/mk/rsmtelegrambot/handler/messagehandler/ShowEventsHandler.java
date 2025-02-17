package net.dunice.mk.rsmtelegrambot.handler.messagehandler;

import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.CANCEL;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.DELETE_EVENT;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.EDIT_EVENT;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.EVENTS_LIST;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.NO;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.TO_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.YES;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.CANCEL_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.EVENT_FIELDS_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.GO_TO_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.OK_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.SELECTION_MENU;
import static net.dunice.mk.rsmtelegrambot.entity.Role.USER;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.BasicStep;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.BasicStep.IN_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.BasicStep.SHOW_EVENTS;
import static net.dunice.mk.rsmtelegrambot.handler.state.ShowEventsState.ShowEventsStep.CONFIRM_EVENT_EDIT;
import static net.dunice.mk.rsmtelegrambot.handler.state.ShowEventsState.ShowEventsStep.CONFIRM_EVENT_DELETE;
import static net.dunice.mk.rsmtelegrambot.handler.state.ShowEventsState.ShowEventsStep.EDIT_EVENT_FIELD;
import static net.dunice.mk.rsmtelegrambot.handler.state.ShowEventsState.ShowEventsStep.HANDLE_USER_ACTION;
import static net.dunice.mk.rsmtelegrambot.handler.state.ShowEventsState.ShowEventsStep.SELECT_EVENT_FIELD;
import static net.dunice.mk.rsmtelegrambot.handler.state.ShowEventsState.ShowEventsStep.SHOW_EVENTS_LIST;
import static net.dunice.mk.rsmtelegrambot.handler.state.ShowEventsState.ShowEventsStep.SHOW_EVENT_DETAILS;

import lombok.RequiredArgsConstructor;
import net.dunice.mk.rsmtelegrambot.constant.Menu;
import net.dunice.mk.rsmtelegrambot.dto.MessageDto;
import net.dunice.mk.rsmtelegrambot.entity.Event;
import net.dunice.mk.rsmtelegrambot.entity.User;
import net.dunice.mk.rsmtelegrambot.handler.MenuGenerator;
import net.dunice.mk.rsmtelegrambot.handler.state.BasicState;
import net.dunice.mk.rsmtelegrambot.handler.state.ShowEventsState;
import net.dunice.mk.rsmtelegrambot.repository.EventRepository;
import net.dunice.mk.rsmtelegrambot.repository.UserRepository;
import org.apache.commons.lang3.StringUtils;
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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

    private static final String EVENT_INFO_TEMPLATE = """
        Мероприятие: %s
        Описание: %s
        Ссылка: %s
        Адрес: %s
        Дата: %s  |  Время: %s
        """;
    private final EventRepository eventRepository;
    private final MenuGenerator menuGenerator;
    private final Map<Long, BasicState> basicStates;
    private final UserRepository userRepository;
    private final Map<Long, ShowEventsState> showEventStates;
    private final Map<Menu, ReplyKeyboard> menus;

    @Override
    public BasicStep getStep() {
        return SHOW_EVENTS;
    }

    @Override
    public PartialBotApiMethod<Message> handle(MessageDto messageDto, Long telegramId) {
        String text = messageDto.getText();
        ShowEventsState state = showEventStates.get(telegramId);
        if (state == null) {
            showEventStates.put(telegramId, (state = new ShowEventsState()));
        }

        return switch (state.getStep()) {
            case SHOW_EVENTS_LIST -> {
                List<Event> events = eventRepository.findAllByEventDateAfterOrderByEventDateAsc(LocalDateTime.now());
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

                        if (isLogoPresent(targetEvent.getLogo())) {
                            SendPhoto sendPhoto = generateImageMessage(telegramId,
                                eventDescription, getUserActionKeyboard(userOptional), targetEvent.getLogo());
                            sendPhoto.setParseMode("HTML");
                            yield sendPhoto;
                        } else {
                            SendMessage sendMessage = generateSendMessage(telegramId,
                                eventDescription, getUserActionKeyboard(userOptional));
                            sendMessage.setParseMode("HTML");
                            yield sendMessage;
                        }
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
                } else if (text.equals(DELETE_EVENT)) {
                    state.setStep(CONFIRM_EVENT_DELETE);
                    yield generateSendMessage(telegramId,
                        String.format("Вы точно хотите удалить меропирятие '%s'?", state.getTargetEvent().getTitle()),
                        menus.get(SELECTION_MENU));
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
                } else if (text.equalsIgnoreCase("Адрес")){
                    state.setStep(EDIT_EVENT_FIELD);
                    state.setEditingFieldName(text);
                    yield generateSendMessage(telegramId,
                        "Введите адрес (улица и номер дома, например Крестьянская 207): ", menus.get(CANCEL_MENU));
                } else if (text.equalsIgnoreCase("Логотип")){
                    state.setStep(EDIT_EVENT_FIELD);
                    state.setEditingFieldName(text);
                    yield generateSendMessage(telegramId,
                        "Отправьте логотип мероприятия (изображение): ", menus.get(CANCEL_MENU));
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
                        case "Адрес" -> {
                            if (text != null && text.length() <= 255) {
                                String address = text.trim();
                                String[] parts = address.split(" ");
                                if (parts.length != 2) {
                                    yield generateSendMessage(telegramId,
                                        "Адрес должен соответствовать формату (улица и номер дома, например Крестьянская 207), Повторите ввод (до 255 символов):",
                                        menus.get(CANCEL_MENU));
                                }
                                if (!parts[1].matches("^[0-9]+[A-Za-zА-Яа-я]?$|^[0-9]+/[0-9]+$|^[0-9]+[A-Za-zА-Яа-я]?/[0-9]+$")) {
                                    yield generateSendMessage(telegramId,
                                        "Номер дома должен быть в формате '123', '123А' или '123/456'. Повторите ввод:",
                                        menus.get(CANCEL_MENU));
                                }
                                address = "г. Майкоп, ул. %s, д. %s".formatted(parts[0], parts[1]);
                                targetEvent.setAddress(address);
                            }
                        }
                        case "Логотип" -> {
                            if (messageDto.getImage() != null) {
                                targetEvent.setLogo(messageDto.getImage());
                            } else {
                                yield generateSendMessage(telegramId,
                                    "Логотип должен быть изображением. Повторите ввод:",
                                    menus.get(CANCEL_MENU));
                            }
                        }
                    }

                    state.setStep(CONFIRM_EVENT_EDIT);
                    if (isLogoPresent(targetEvent.getLogo())) {
                        SendPhoto sendPhoto = generateImageMessage(telegramId,
                            "Мероприятие с новыми данными:\n" + getEventDescription(targetEvent) + "\nСохранить изменения?",
                            menus.get(SELECTION_MENU), targetEvent.getLogo());
                        sendPhoto.setParseMode("HTML");
                        yield sendPhoto;
                    } else {
                        SendMessage sendMessage = generateSendMessage(telegramId,
                            "Мероприятие с новыми данными:\n" + getEventDescription(targetEvent) + "\nСохранить изменения?",
                            menus.get(SELECTION_MENU));
                        sendMessage.setParseMode("HTML");
                        yield sendMessage;
                    }
                } catch (DateTimeParseException e) {
                    yield generateSendMessage(telegramId,
                        "Дата должна быть в формате (ДД.ММ.ГГГГ-ЧЧ:ММ). Повторите ввод:");
                } catch (Exception e) {
                    yield generateSendMessage(telegramId,
                        "Ошибка при редактировании поля. Повторите ввод:",
                        menus.get(CANCEL_MENU));
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
                } else {
                    yield generateSendMessage(telegramId, "Неверная команда.", menus.get(SELECTION_MENU));
                }
            }

            case CONFIRM_EVENT_DELETE -> {
                String responseMessage;
                state.setStep(SHOW_EVENTS_LIST);

                if (YES.equalsIgnoreCase(text)) {
                    eventRepository.delete(state.getTargetEvent());
                    responseMessage = String.format("Мероприятие '%s' удалено.", state.getTargetEvent().getTitle());
                } else if (NO.equalsIgnoreCase(text)) {
                    responseMessage = "Удаление мероприятия отменено.";
                } else {
                    responseMessage = "Неверная команда.";
                }
                yield generateSendMessage(telegramId, responseMessage,
                    menus.get(OK_MENU));
            }
        };
    }

    private String getEventDescription(Event targetEvent) {
        return EVENT_INFO_TEMPLATE.formatted(
            targetEvent.getTitle(),
            targetEvent.getText(),
            targetEvent.getLink(),
            getHyperlinkFromAddress(targetEvent.getAddress()),
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
            row.add("%s\n%s | ID: %s".formatted(
                event.getTitle(),
                event.getEventDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                event.getEventId()));
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
            InlineKeyboardButton deleteEventButton = new InlineKeyboardButton(DELETE_EVENT);
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
        BasicState state = basicStates.get(telegramId);
        showEventStates.remove(telegramId);
        state.setStep(IN_MAIN_MENU);
        return menuGenerator.generateRoleSpecificMainMenu(telegramId,
            state.getUser().getUserRole());
    }

    private String getHyperlinkFromAddress(String address) {
        String encodedAddress = URLEncoder.encode(address, StandardCharsets.UTF_8);
        String url = "https://yandex.ru/maps/?text=" + encodedAddress;
        return String.format("<a href=\"%s\">%s</a>", url, address);
    }

    private boolean isLogoPresent(byte[] logo) {
        return logo != null && logo.length > 0;
    }
}