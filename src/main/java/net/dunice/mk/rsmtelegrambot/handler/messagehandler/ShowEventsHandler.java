package net.dunice.mk.rsmtelegrambot.handler.messagehandler;

import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.DELETE;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.EVENTS_LIST;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.TO_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.IN_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.SHOW_EVENTS;
import static net.dunice.mk.rsmtelegrambot.handler.state.step.ShowEventsStep.SHOW_EVENTS_LIST;
import static net.dunice.mk.rsmtelegrambot.handler.state.step.ShowEventsStep.SHOW_EVENT_DETAILS;

import lombok.RequiredArgsConstructor;
import net.dunice.mk.rsmtelegrambot.dto.MessageDto;
import net.dunice.mk.rsmtelegrambot.entity.Event;
import net.dunice.mk.rsmtelegrambot.handler.MenuGenerator;
import net.dunice.mk.rsmtelegrambot.handler.state.BasicState;
import net.dunice.mk.rsmtelegrambot.handler.state.step.ShowEventsStep;
import net.dunice.mk.rsmtelegrambot.repository.EventRepository;
import net.dunice.mk.rsmtelegrambot.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
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
public class ShowEventsHandler implements MessageHandler {

    private final EventRepository eventRepository;
    private final MenuGenerator menuGenerator;
    private final Map<Long, BasicState> basicStates;
    private final UserRepository userRepository;
    private final Map<Long, ShowEventsStep> showEventSteps;
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
        ShowEventsStep step = showEventSteps.get(telegramId);
        if (step == null) {
            showEventSteps.put(telegramId, (step = SHOW_EVENTS_LIST));
        }

        if (TO_MAIN_MENU.equalsIgnoreCase(text)) {
            return goToMainMenu(telegramId);
        }
        else if (EVENTS_LIST.equalsIgnoreCase(text)) {
            showEventSteps.put(telegramId, SHOW_EVENTS_LIST);
            step = SHOW_EVENTS_LIST;
        }
        else if (text.startsWith(DELETE)) {
            Integer eventId = Integer.valueOf(text.substring(text.lastIndexOf(' ')+1));
            eventRepository.deleteById(eventId);
            showEventSteps.put(telegramId, SHOW_EVENTS_LIST);
            step = SHOW_EVENTS_LIST;
        }

        return switch (step) {
            case SHOW_EVENTS_LIST -> {
                List<Event> events = eventRepository.findAll();
                showEventSteps.put(telegramId, SHOW_EVENT_DETAILS);
                yield generateSendMessage(telegramId, "Выберите интересующее вас мероприятие: ",
                    generateEventListKeyboard(events));
            }
            case SHOW_EVENT_DETAILS -> {
                Integer eventId = null;
                try {
                    eventId = Integer.valueOf(text.substring(text.lastIndexOf(' ')+1));
                } catch (NumberFormatException e) {
                    yield generateSendMessage(telegramId, "Нет мероприятия с таким ID");
                }
                Optional<Event> eventOptional = eventRepository.findById(eventId);
                if (eventOptional.isPresent()) {
                    Event event = eventOptional.get();
                    String eventDescription = EVENT_INFO_TEMPLATE.formatted(
                        event.getTitle(),
                        event.getText(),
                        event.getLink(),
                        event.getEventDate().toLocalDate(),
                        event.getEventDate().toLocalTime());
                    yield generateSendMessage(telegramId, eventDescription, generateGoBackKeyboard(event.getEventId()));
                } else {
                    yield generateSendMessage(telegramId, "Нет мероприятия с таким ID");
                }
            }
        };
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

    private ReplyKeyboard generateGoBackKeyboard(Integer eventId) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        InlineKeyboardButton toMainMenuButton = new InlineKeyboardButton(TO_MAIN_MENU);
        toMainMenuButton.setCallbackData(toMainMenuButton.getText());
        InlineKeyboardButton toEventsButton = new InlineKeyboardButton(EVENTS_LIST);
        toEventsButton.setCallbackData(toEventsButton.getText());
        InlineKeyboardButton deleteEventButton = new InlineKeyboardButton("%s | ID: %s".formatted(DELETE, eventId));
        deleteEventButton.setCallbackData(deleteEventButton.getText());
        keyboard.add(List.of(toMainMenuButton));
        keyboard.add(List.of(toEventsButton));
        keyboard.add(List.of(deleteEventButton));
        inlineKeyboardMarkup.setKeyboard(keyboard);
        return inlineKeyboardMarkup;
    }

    private SendMessage goToMainMenu(Long telegramId) {
        showEventSteps.remove(telegramId);
        basicStates.put(telegramId, IN_MAIN_MENU);
        return menuGenerator.generateRoleSpecificMainMenu(telegramId,
            userRepository.findByTelegramId(telegramId).get().getUserRole());
    }
}