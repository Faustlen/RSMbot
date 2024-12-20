package net.dunice.mk.rsmtelegrambot.handler.state.stateobject;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
public class EventCreationState {
    private EventCreationStep step = EventCreationStep.REQUEST_EVENT_NAME;
    private String eventName;
    private String eventDescription;
    private LocalDateTime eventDateTime;
    private String eventLink;

    public enum EventCreationStep {
        REQUEST_EVENT_NAME,
        VERIFY_EVENT_NAME,
        VERIFY_EVENT_DESCRIPTION,
        VERIFY_EVENT_DATE_TIME,
        VERIFY_EVENT_LINK,
        CONFIRM_EVENT,
        CHECK_CONFIRMATION,
    }
}
