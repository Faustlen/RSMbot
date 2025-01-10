package net.dunice.mk.rsmtelegrambot.handler.state.stateobject;

import lombok.Getter;
import lombok.Setter;
import net.dunice.mk.rsmtelegrambot.entity.Event;

@Getter
@Setter
public class ShowEventsState {

    private ShowEventsStep step = ShowEventsStep.SHOW_EVENTS_LIST;
    private Event targetEvent;
    private String editingFieldName;

    public enum ShowEventsStep {
        SHOW_EVENTS_LIST,
        SHOW_EVENT_DETAILS,
        HANDLE_USER_ACTION,
        SELECT_EVENT_FIELD,
        EDIT_EVENT_FIELD,
        CONFIRM_EVENT_EDIT,
    }
}
