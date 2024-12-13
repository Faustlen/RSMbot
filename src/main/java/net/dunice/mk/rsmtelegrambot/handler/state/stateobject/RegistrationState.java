package net.dunice.mk.rsmtelegrambot.handler.state.stateobject;

import lombok.Getter;
import lombok.Setter;
import net.dunice.mk.rsmtelegrambot.handler.state.stateobject.step.RegistrationStep;

@Setter
@Getter
public class RegistrationState {
    private RegistrationStep step = RegistrationStep.CONFIRM;
    private String fullName;
    private String name;
    private String phoneNumber;
    private String info;
}
