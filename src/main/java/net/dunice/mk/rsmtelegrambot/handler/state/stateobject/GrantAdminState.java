package net.dunice.mk.rsmtelegrambot.handler.state.stateobject;

import lombok.Getter;
import lombok.Setter;
import net.dunice.mk.rsmtelegrambot.entity.User;
import net.dunice.mk.rsmtelegrambot.handler.state.step.GrantAdminStep;

import static net.dunice.mk.rsmtelegrambot.handler.state.step.GrantAdminStep.REQUEST_USER_ID;

@Getter
@Setter
public class GrantAdminState {
    private User targetUser;
    private GrantAdminStep step = REQUEST_USER_ID;
}
