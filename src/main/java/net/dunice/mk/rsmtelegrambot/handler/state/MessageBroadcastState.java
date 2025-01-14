package net.dunice.mk.rsmtelegrambot.handler.state;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MessageBroadcastState {

    private String text;
    private MessageBroadcastStep step = MessageBroadcastStep.REQUEST_MESSAGE_TEXT;

    public enum MessageBroadcastStep {
        REQUEST_MESSAGE_TEXT,
        VERIFY_MESSAGE_TEXT,
        CONFIRM_MESSAGE_TEXT,
    }
}
