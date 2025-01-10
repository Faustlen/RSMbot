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
        VALIDATE_EVENT_NAME,
        VALIDATE_EVENT_DESCRIPTION,
        VALIDATE_EVENT_DATE_TIME,
        VALIDATE_EVENT_LINK,
        CONFIRM_EVENT,
        FINISH
    }
}
