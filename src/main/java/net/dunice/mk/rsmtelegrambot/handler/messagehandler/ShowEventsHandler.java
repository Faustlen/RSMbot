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
        ShowEventsState state = showEventStates.get(telegramId);
        if (state == null) {
            state = new ShowEventsState();
            showEventStates.put(telegramId, state);
        }

        if (TO_MAIN_MENU.equalsIgnoreCase(messageDto.getText())) {
            return goToMainMenu(telegramId);
        }

        return switch (state.getStep()) {
            case SHOW_EVENTS_LIST -> handleShowEventsList(telegramId, state);
            case SHOW_EVENT_DETAILS -> handleShowEventDetails(messageDto, telegramId, state);
            case HANDLE_USER_ACTION -> handleUserAction(messageDto, telegramId, state);
            case SELECT_EVENT_FIELD -> handleSelectEventField(messageDto, telegramId, state);
            case EDIT_EVENT_FIELD -> handleEditEventField(messageDto, telegramId, state);
            case CONFIRM_EVENT_EDIT -> handleConfirmEventEdit(messageDto, telegramId, state);
            case CONFIRM_EVENT_DELETE -> handleConfirmEventDelete(messageDto, telegramId, state);
        };
    }

    private PartialBotApiMethod<Message> handleShowEventsList(Long telegramId, ShowEventsState state) {
        List<Event> events = eventRepository.findAllByEventDateAfterOrderByEventDateAsc(LocalDateTime.now());
        state.setStep(SHOW_EVENT_DETAILS);

        return generateSendMessage(
            telegramId,
            "Выберите интересующее вас мероприятие:",
            generateEventListKeyboard(events)
        );
    }

    private PartialBotApiMethod<Message> handleShowEventDetails(MessageDto messageDto,
                                                                Long telegramId,
                                                                ShowEventsState state) {
        String text = messageDto.getText();

        if (text == null) {
            return handleShowEventsList(telegramId, state);
        }

        try {
            Integer eventId = Integer.valueOf(text.substring(text.lastIndexOf(' ') + 1));
            Optional<Event> eventOptional = eventRepository.findById(eventId);

            if (eventOptional.isPresent()) {
                Event targetEvent = eventOptional.get();
                state.setTargetEvent(targetEvent);
                state.setStep(HANDLE_USER_ACTION);
                String eventDescription = getEventDescription(targetEvent);

                Optional<User> userOptional = userRepository.findById(telegramId);

                if (isLogoPresent(targetEvent.getLogo())) {
                    SendPhoto sendPhoto = generateImageMessage(
                        telegramId,
                        eventDescription,
                        getUserActionKeyboard(userOptional),
                        targetEvent.getLogo()
                    );
                    sendPhoto.setParseMode("HTML");
                    return sendPhoto;
                } else {
                    SendMessage sendMessage = generateSendMessage(
                        telegramId,
                        eventDescription,
                        getUserActionKeyboard(userOptional)
                    );
                    sendMessage.setParseMode("HTML");
                    return sendMessage;
                }
            } else {
                return generateSendMessage(telegramId, "Мероприятие не найдено.");
            }
        } catch (NumberFormatException e) {
            return generateSendMessage(telegramId, "Неверный ID мероприятия.");
        }
    }

    private PartialBotApiMethod<Message> handleUserAction(MessageDto messageDto,
                                                          Long telegramId,
                                                          ShowEventsState state) {
        String text = messageDto.getText();
        if (EVENTS_LIST.equalsIgnoreCase(text)) {
            state.setStep(SHOW_EVENTS_LIST);
            return handleShowEventsList(telegramId, state);
        } else if (DELETE_EVENT.equalsIgnoreCase(text)) {
            state.setStep(CONFIRM_EVENT_DELETE);
            return generateSendMessage(
                telegramId,
                String.format("Вы точно хотите удалить меропирятие '%s'?", state.getTargetEvent().getTitle()),
                menus.get(SELECTION_MENU)
            );
        } else if (EDIT_EVENT.equalsIgnoreCase(text)) {
            state.setStep(SELECT_EVENT_FIELD);
            return generateSendMessage(
                telegramId,
                "Выберите поле, которое хотите отредактировать:",
                menus.get(EVENT_FIELDS_MENU)
            );
        } else {
            return generateSendMessage(telegramId, "Неверная команда", menus.get(GO_TO_MAIN_MENU));
        }
    }

    private PartialBotApiMethod<Message> handleSelectEventField(MessageDto messageDto,
                                                                Long telegramId,
                                                                ShowEventsState state) {
        String text = messageDto.getText();

        if (isFieldName(text)) {
            state.setStep(EDIT_EVENT_FIELD);
            state.setEditingFieldName(text);
            String question = switch (text) {
                case "Название" -> "Введите новое название мероприятия:";
                case "Описание" -> "Введите новое описание мероприятиям";
                case "Дата и Время" ->
                    "Введите новую дату мероприятия в формате (ДД.ММ.ГГГГ-ЧЧ:ММ):";
                case "Ссылка" -> "Введите новую ссылку:";
                case "Адрес" -> "Введите адрес (улица и номер дома, например Крестьянская 207):";
                case "Логотип" -> "Отправьте логотип мероприятия (изображение):";
                default -> "Поле неизвестно";
            };
            return generateSendMessage(telegramId, question, menus.get(CANCEL_MENU));
        } else {
            return generateSendMessage(
                telegramId,
                "Неверное поле. Выберите одно из доступных.",
                menus.get(EVENT_FIELDS_MENU)
            );
        }
    }

    private PartialBotApiMethod<Message> handleEditEventField(MessageDto messageDto,
                                                              Long telegramId,
                                                              ShowEventsState state) {
        String text = messageDto.getText();
        if (CANCEL.equalsIgnoreCase(text) || text == null) {
            state.setStep(SHOW_EVENT_DETAILS);
            messageDto.setText(" " + state.getTargetEvent().getEventId());
            return handleShowEventDetails(messageDto, telegramId, state);
        }

        Event targetEvent = state.getTargetEvent();
        try {
            switch (state.getEditingFieldName()) {
                case "Название" -> targetEvent.setTitle(text.trim());
                case "Описание" -> targetEvent.setText(text.trim());
                case "Дата и Время" ->
                    targetEvent.setEventDate(LocalDateTime.parse(
                        text.trim(),
                        DateTimeFormatter.ofPattern("dd.MM.yyyy-HH:mm"))
                    );
                case "Ссылка" -> targetEvent.setLink(text.trim());
                case "Адрес" -> {
                    if (!validateAddress(text)) {
                        return generateSendMessage(
                            telegramId,
                            "Адрес должен соответствовать формату (улица и номер дома, например Крестьянская 207). Повторите ввод:",
                            menus.get(CANCEL_MENU)
                        );
                    }
                    targetEvent.setAddress(formatAddress(text.trim()));
                }
                case "Логотип" -> {
                    if (messageDto.getImage() != null) {
                        targetEvent.setLogo(messageDto.getImage());
                    } else {
                        return generateSendMessage(
                            telegramId,
                            "Логотип должен быть изображением. Повторите ввод:",
                            menus.get(CANCEL_MENU)
                        );
                    }
                }
                default -> {
                    return generateSendMessage(telegramId, "Неверное поле", menus.get(CANCEL_MENU));
                }
            }
            state.setStep(CONFIRM_EVENT_EDIT);
            return buildEventConfirmMessage(telegramId, targetEvent);

        } catch (DateTimeParseException e) {
            return generateSendMessage(
                telegramId,
                "Дата должна быть в формате (ДД.ММ.ГГГГ-ЧЧ:ММ). Повторите ввод:",
                menus.get(CANCEL_MENU)
            );
        } catch (Exception e) {
            return generateSendMessage(
                telegramId,
                "Ошибка при редактировании поля. Повторите ввод:",
                menus.get(CANCEL_MENU)
            );
        }
    }

    private PartialBotApiMethod<Message> handleConfirmEventEdit(MessageDto messageDto,
                                                                Long telegramId,
                                                                ShowEventsState state) {
        String text = messageDto.getText();
        if (YES.equalsIgnoreCase(text)) {
            eventRepository.save(state.getTargetEvent());
        } else if (!NO.equalsIgnoreCase(text)) {
            return buildEventConfirmMessage(telegramId, state.getTargetEvent());
        }
        state.setStep(SHOW_EVENT_DETAILS);
        messageDto.setText(" " + state.getTargetEvent().getEventId());
        return handleShowEventDetails(messageDto, telegramId, state);
    }

    private PartialBotApiMethod<Message> handleConfirmEventDelete(MessageDto messageDto,
                                                                  Long telegramId,
                                                                  ShowEventsState state) {
        String text = messageDto.getText();
        String response;
        state.setStep(SHOW_EVENTS_LIST);

        if (YES.equalsIgnoreCase(text)) {
            eventRepository.delete(state.getTargetEvent());
            response = String.format("Мероприятие '%s' удалено.", state.getTargetEvent().getTitle());
        } else if (NO.equalsIgnoreCase(text)) {
            response = "Удаление мероприятия отменено.";
        } else {
            response = "Неверная команда.";
        }

        return generateSendMessage(telegramId, response, menus.get(OK_MENU));
    }

    private String getEventDescription(Event targetEvent) {
        return EVENT_INFO_TEMPLATE.formatted(
            targetEvent.getTitle(),
            targetEvent.getText(),
            targetEvent.getLink(),
            getHyperlinkFromAddress(targetEvent.getAddress()),
            targetEvent.getEventDate().toLocalDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
            targetEvent.getEventDate().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
        );
    }

    private ReplyKeyboardMarkup generateEventListKeyboard(List<Event> events) {
        ReplyKeyboardMarkup replyMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow mainMenuRow = new KeyboardRow();
        mainMenuRow.add(TO_MAIN_MENU);
        keyboard.add(mainMenuRow);

        for (Event event : events) {
            KeyboardRow row = new KeyboardRow();
            row.add("%s\n%s | ID: %s".formatted(
                event.getTitle(),
                event.getEventDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                event.getEventId())
            );
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
        toMainMenuButton.setCallbackData(TO_MAIN_MENU);

        InlineKeyboardButton toEventsButton = new InlineKeyboardButton(EVENTS_LIST);
        toEventsButton.setCallbackData(EVENTS_LIST);

        keyboard.add(List.of(toMainMenuButton));
        keyboard.add(List.of(toEventsButton));

        if (userOptional.isPresent() && userOptional.get().getUserRole() != USER) {
            InlineKeyboardButton deleteButton = new InlineKeyboardButton(DELETE_EVENT);
            deleteButton.setCallbackData(DELETE_EVENT);

            InlineKeyboardButton editButton = new InlineKeyboardButton(EDIT_EVENT);
            editButton.setCallbackData(EDIT_EVENT);

            keyboard.add(List.of(deleteButton));
            keyboard.add(List.of(editButton));
        }

        inlineKeyboardMarkup.setKeyboard(keyboard);
        return inlineKeyboardMarkup;
    }

    private SendMessage goToMainMenu(Long telegramId) {
        BasicState state = basicStates.get(telegramId);
        showEventStates.remove(telegramId);
        state.setStep(IN_MAIN_MENU);
        return menuGenerator.generateRoleSpecificMainMenu(telegramId, state.getUser().getUserRole());
    }

    private String getHyperlinkFromAddress(String address) {
        String encodedAddress = URLEncoder.encode(address, StandardCharsets.UTF_8);
        String url = "https://yandex.ru/maps/?text=" + encodedAddress;
        return String.format("<a href=\"%s\">%s</a>", url, address);
    }

    private boolean isLogoPresent(byte[] logo) {
        return logo != null && logo.length > 0;
    }

    private PartialBotApiMethod<Message> buildEventConfirmMessage(Long telegramId, Event targetEvent) {
        String text = "Мероприятие с новыми данными:\n" +
            getEventDescription(targetEvent) +
            "\nСохранить изменения?";

        if (isLogoPresent(targetEvent.getLogo())) {
            SendPhoto sendPhoto = generateImageMessage(telegramId, text, menus.get(SELECTION_MENU), targetEvent.getLogo());
            sendPhoto.setParseMode("HTML");
            return sendPhoto;
        } else {
            SendMessage sendMessage = generateSendMessage(telegramId, text, menus.get(SELECTION_MENU));
            sendMessage.setParseMode("HTML");
            return sendMessage;
        }
    }

    private boolean validateAddress(String text) {
        if (text == null || text.length() > 255) {
            return false;
        }
        String[] parts = text.split(" ");
        if (parts.length != 2) {
            return false;
        }
        return parts[1].matches("^[0-9]+[A-Za-zА-Яа-я]?$|^[0-9]+/[0-9]+$|^[0-9]+[A-Za-zА-Яа-я]?/[0-9]+$");
    }

    private String formatAddress(String text) {
        String[] parts = text.split(" ");
        return "г. Майкоп, ул. %s, д. %s".formatted(parts[0], parts[1]);
    }

    private boolean isFieldName(String text) {
        if (text == null) {
            return false;
        }
        return List.of("Название", "Описание", "Дата и Время", "Ссылка", "Адрес", "Логотип")
            .contains(text);
    }
}
