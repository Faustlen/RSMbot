package net.dunice.mk.rsmtelegrambot.handler.state.stateobject;

import lombok.Getter;
import lombok.Setter;
import net.dunice.mk.rsmtelegrambot.entity.User;

@Getter
@Setter
public class GrantAdminState {
    private User targetUser;
    private GrantAdminStep step = GrantAdminStep.REQUEST_USER_ID;

    public enum GrantAdminStep {
        REQUEST_USER_ID,
        VERIFY_ADMIN_CANDIDATE,
        CONFIRM_ADMIN_CANDIDATE,
    }
}
