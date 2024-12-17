package net.dunice.mk.rsmtelegrambot.handler.state.stateobject;

import lombok.Getter;
import lombok.Setter;
import net.dunice.mk.rsmtelegrambot.handler.state.step.UserRegistrationStep;

@Setter
@Getter
public class UserRegistrationState {
    private UserRegistrationStep step = UserRegistrationStep.REQUEST_FULL_NAME;
    private String fullName;
    private String name;
    private String phoneNumber;
    private String info;
}
