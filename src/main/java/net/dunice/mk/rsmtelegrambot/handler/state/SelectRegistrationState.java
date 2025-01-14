package net.dunice.mk.rsmtelegrambot.handler.state;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SelectRegistrationState {

    private SelectRegistrationStep step = SelectRegistrationStep.REQUEST_REGISTRATION;

    public enum SelectRegistrationStep {
        REQUEST_REGISTRATION,
        CHECK_CONFIRMATION,
        SWITCH_REGISTRATION_TYPE,
        RETRY_REGISTRATION,
    }
}
