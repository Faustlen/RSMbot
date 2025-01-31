package net.dunice.mk.rsmtelegrambot.handler.state;

import lombok.Getter;
import lombok.Setter;
import net.dunice.mk.rsmtelegrambot.entity.User;
import java.util.List;

@Getter
@Setter
public class ShowAdminsState {

    private User targetUser;
    private List<User> allUsers;
    private List<User> usersToDisplay;
    private ShowAdminsStep step = ShowAdminsStep.SHOW_ADMINS_LIST;
    private int page = 0;

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

    public enum ShowAdminsStep {
        SHOW_ADMINS_LIST,
        SHOW_ADMIN_DETAILS,
        HANDLE_USER_ACTION,
        GRANT_OR_REVOKE_ADMIN_RIGHTS,
        FINISH
    }
}
