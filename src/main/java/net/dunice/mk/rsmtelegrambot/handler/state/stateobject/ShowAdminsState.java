package net.dunice.mk.rsmtelegrambot.handler.state.stateobject;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShowAdminsState {

    private ShowAdminsStep step = ShowAdminsStep.SHOW_ADMINS_LIST;

    public enum ShowAdminsStep {
        SHOW_ADMINS_LIST,
    }
}
