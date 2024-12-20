package net.dunice.mk.rsmtelegrambot.handler.state.stateobject;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UpdateProfileState {
    private UpdateProfileStep step = UpdateProfileStep.CONFIRM;
    private String fullName;
    private String name;
    private String phoneNumber;
    private String info;

    public enum UpdateProfileStep {
        CONFIRM,
        FULL_NAME,
        PHONE_NUMBER,
        INFO
    }
}
