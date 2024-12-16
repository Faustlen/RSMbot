package net.dunice.mk.rsmtelegrambot.handler.state.stateobject;

import lombok.Getter;
import lombok.Setter;
import net.dunice.mk.rsmtelegrambot.handler.state.step.UpdateProfileStep;

@Setter
@Getter
public class UpdateProfileState {
    private UpdateProfileStep step = UpdateProfileStep.CONFIRM;
    private String fullName;
    private String name;
    private String phoneNumber;
    private String info;

}
