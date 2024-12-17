package net.dunice.mk.rsmtelegrambot.handler.messagehandler;

import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.TO_EVENTS_LIST;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.TO_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.IN_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.SHOW_EVENTS;
import static net.dunice.mk.rsmtelegrambot.handler.state.step.ShowEventsStep.FINISH;
import static net.dunice.mk.rsmtelegrambot.handler.state.step.ShowEventsStep.SHOW_EVENTS_LIST;
import static net.dunice.mk.rsmtelegrambot.handler.state.step.ShowEventsStep.SHOW_EVENT_DETAILS;

import lombok.RequiredArgsConstructor;
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

        ShowEventsStep step = showEventSteps.get(telegramId);
        if (step == null) {
            showEventSteps.put(telegramId, (step = SHOW_EVENTS_LIST));
        }

        return switch (step) {
            case SHOW_EVENTS_LIST -> {
                List<Event> events = eventRepository.findAll();
                showEventSteps.put(telegramId, SHOW_EVENT_DETAILS);
                yield generateSendMessage(telegramId, "Выберите интересующее вас событие: ",
                    generateEventListKeyboard(events));
            }
            case SHOW_EVENT_DETAILS -> {
                if (TO_MAIN_MENU.equalsIgnoreCase(message)) {
                    showEventSteps.remove(telegramId);
                    basicStates.put(telegramId, IN_MAIN_MENU);
                    yield menuGenerator.generateRoleSpecificMainMenu(telegramId,
                        userRepository.findByTelegramId(telegramId).get().getUserRole());
                } else {
                    Optional<Event> eventOptional = eventRepository.findByTitle(message);
                    if (eventOptional.isPresent()) {
                        Event event = eventOptional.get();
                        String eventDescription = String.format(DESCRIPTION_TEMPLATE,
                            event.getTitle(),
                            event.getText(),
                            event.getLink(),
                            event.getEventDate().toLocalDate(),
                            event.getEventDate().toLocalTime());
                        showEventSteps.put(telegramId, FINISH);
                        yield generateSendMessage(telegramId, eventDescription, generateGoBackKeyboard());
                    } else {
                        yield generateSendMessage(telegramId, "Нет мероприятия с таким названием");
                    }
                }
            }
            case FINISH -> {
                if (TO_MAIN_MENU.equalsIgnoreCase(message)) {
                    showEventSteps.remove(telegramId);
                    basicStates.put(telegramId, IN_MAIN_MENU);
                    yield menuGenerator.generateRoleSpecificMainMenu(telegramId,
                        userRepository.findByTelegramId(telegramId).get().getUserRole());
                } else if (TO_EVENTS_LIST.equalsIgnoreCase(message)) {
                    showEventSteps.put(telegramId, SHOW_EVENTS_LIST);
                    yield handle(message, telegramId);
                } else {
                    yield generateSendMessage(telegramId, "Неверная команда");
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
            row.add(events.get(i++).getTitle());
            keyboard.add(row);
        }
        replyMarkup.setKeyboard(keyboard);
        replyMarkup.setResizeKeyboard(true);
        replyMarkup.setOneTimeKeyboard(false);
        return replyMarkup;
    }

    private ReplyKeyboard generateGoBackKeyboard() {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        InlineKeyboardButton toMainMenuButton = new InlineKeyboardButton(TO_MAIN_MENU);
        toMainMenuButton.setCallbackData(toMainMenuButton.getText());
        InlineKeyboardButton toEventsButton = new InlineKeyboardButton(TO_EVENTS_LIST);
        toEventsButton.setCallbackData(toEventsButton.getText());
        keyboard.add(List.of(toMainMenuButton));
        keyboard.add(List.of(toEventsButton));
        inlineKeyboardMarkup.setKeyboard(keyboard);
        return inlineKeyboardMarkup;
    }
}