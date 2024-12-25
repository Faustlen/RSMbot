package net.dunice.mk.rsmtelegrambot.handler.state.stateobject;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UpdateProfileState {
    private UpdateProfileStep step = UpdateProfileStep.REQUEST_USER_INFO;
    private String info;

    public enum UpdateProfileStep {
        REQUEST_USER_INFO,
        VERIFY_USER_INFO
    }
}
