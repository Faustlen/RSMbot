package net.dunice.mk.rsmtelegrambot.handler.state.stateobject;

import lombok.Getter;
import lombok.Setter;
import net.dunice.mk.rsmtelegrambot.entity.User;

@Getter
@Setter
public class ShowUsersState {

    private User targetUser;
    private ShowUsersStep step = ShowUsersStep.SHOW_USERS_LIST;

    public enum ShowUsersStep {
        SHOW_USERS_LIST,
        SHOW_USER_DETAILS,
        BAN_OR_UNBAN,
        HANDLE_USER_ACTION
    }
}
