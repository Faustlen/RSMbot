package net.dunice.mk.rsmtelegrambot.handler.messagehandler;

import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.SHOW_EVENTS;
import static net.dunice.mk.rsmtelegrambot.handler.state.stateobject.step.ShowEventsStep.DETAILS;
import static net.dunice.mk.rsmtelegrambot.handler.state.stateobject.step.ShowEventsStep.LIST;

import lombok.RequiredArgsConstructor;
import net.dunice.mk.rsmtelegrambot.handler.state.BasicState;
import net.dunice.mk.rsmtelegrambot.constant.Menu;
import net.dunice.mk.rsmtelegrambot.handler.state.stateobject.step.ShowEventsStep;
import net.dunice.mk.rsmtelegrambot.entity.Event;
import net.dunice.mk.rsmtelegrambot.handler.MenuGenerator;
import net.dunice.mk.rsmtelegrambot.repository.EventRepository;
import net.dunice.mk.rsmtelegrambot.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ShowEventsHandler implements MessageHandler {

    private final EventRepository eventRepository;
    private final EnumMap<Menu, ReplyKeyboardMarkup> menus;
    private final MenuGenerator menuGenerator;
    private final Map<Long, BasicState> states;
    private final UserRepository userRepository;
    private final Map<Long, ShowEventsStep> showEventSteps;
    private static final String DESCRIPTION_TEMPLATE = """
        Мероприятие - %s
        Описание - %s
        Ссылка - %s
        Дата - %s  |  Время - %s
        """;

    @Override
    public BasicState getState() {
        return SHOW_EVENTS;
    }

    @Override
    public SendMessage handle(String message, Long telegramId) {
        states.put(telegramId, SHOW_EVENTS);

        if ("В главное меню".equalsIgnoreCase(message)) {
            cleanStates(telegramId);
            return menuGenerator.generateRoleSpecificMainMenu(telegramId, userRepository.findByTelegramId(telegramId).get().getUserRole());
        }

        ShowEventsStep state = showEventSteps.get(telegramId);
        if (state == null || message.equalsIgnoreCase("К списку мероприятий")) {
            showEventSteps.put(telegramId, (state = LIST));
        }

        return switch (state) {
            case LIST -> {
                List<Event> events = eventRepository.findAll();
                showEventSteps.put(telegramId, DETAILS);
                yield generateSendMessage(telegramId, "Выберите интересующее вас событие: ",
                    generateEventListKeyboard(events));
            }
            case DETAILS -> {
                Optional<Event> eventOptional = eventRepository.findByTitle(message);
                if (eventOptional.isPresent()) {
                    Event event = eventOptional.get();
                    String eventDescription = String.format(DESCRIPTION_TEMPLATE,
                        event.getTitle(),
                        event.getText(),
                        event.getLink(),
                        event.getEventDate().toLocalDate(),
                        event.getEventDate().toLocalTime());
                    yield generateSendMessage(telegramId, eventDescription, goBackKeyboard());
                } else {
                    yield generateSendMessage(telegramId, "Нет мероприятия с таким названием");
                }
            }
        };
    }

    private ReplyKeyboardMarkup generateEventListKeyboard(List<Event> events) {
        ReplyKeyboardMarkup replyMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow firstRow = new KeyboardRow();
        firstRow.add("В главное меню");
        keyboard.add(firstRow);
        for (int i = 0; i < events.size(); ) {
            KeyboardRow row = new KeyboardRow();
            row.add(events.get(i++).getTitle());
            if (i < events.size()) {
                row.add(events.get(i++).getTitle());
            }
            keyboard.add(row);
        }
        replyMarkup.setKeyboard(keyboard);
        replyMarkup.setResizeKeyboard(true);
        replyMarkup.setOneTimeKeyboard(false);
        return replyMarkup;
    }

    private ReplyKeyboardMarkup goBackKeyboard() {
        ReplyKeyboardMarkup replyMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add("В главное меню");
        row.add("К списку мероприятий");
        keyboard.add(row);
        replyMarkup.setKeyboard(keyboard);
        replyMarkup.setResizeKeyboard(true);
        replyMarkup.setOneTimeKeyboard(false);
        return replyMarkup;
    }

    private void cleanStates(Long telegramId) {
        showEventSteps.remove(telegramId);
        states.remove(telegramId);
    }
}
