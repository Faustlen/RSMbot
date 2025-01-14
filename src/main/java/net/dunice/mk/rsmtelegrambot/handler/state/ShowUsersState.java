package net.dunice.mk.rsmtelegrambot.handler.state;

import lombok.Getter;
import lombok.Setter;
import net.dunice.mk.rsmtelegrambot.entity.User;

import java.util.List;

@Getter
@Setter
public class ShowUsersState {

    private User targetUser;
    private List<User> allUsers;
    private List<User> usersToDisplay;
    private ShowUsersStep step = ShowUsersStep.SHOW_USERS_LIST;
    private int page = 0;

    public enum ShowUsersStep {
        SHOW_USERS_LIST,
        SHOW_USER_DETAILS,
        BAN_OR_UNBAN,
        DELETE_USER,
        GRANT_OR_REVOKE_ADMIN_RIGHTS,
        HANDLE_USER_ACTION,
        FINISH
    }
    public void incrementPage() {
        if (page * 10 + 10 <= allUsers.size()) {
            page++;
        }
    }
    public void decrementPage() {
        if (page > 0) {
            page--;
        }
    }
}
