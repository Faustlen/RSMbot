package net.dunice.mk.rsmtelegrambot.handler.state;

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
    private String address;

    public enum EventCreationStep {
        REQUEST_EVENT_NAME,
        VALIDATE_EVENT_NAME,
        VALIDATE_EVENT_DESCRIPTION,
        VALIDATE_EVENT_DATE_TIME,
        VALIDATE_EVENT_LINK,
        VALIDATE_EVENT_ADDRESS,
        CONFIRM_EVENT,
        FINISH
    }
}
