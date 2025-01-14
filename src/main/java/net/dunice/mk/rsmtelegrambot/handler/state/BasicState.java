package net.dunice.mk.rsmtelegrambot.handler.state;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.dunice.mk.rsmtelegrambot.entity.User;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BasicState {

    private User user;

    private BasicStep step;

    public enum BasicStep {
        SELECT_REGISTRATION_TYPE,
        USER_REGISTRATION,
        PARTNER_REGISTRATION,
        IN_MAIN_MENU,
        IN_PARTNER_MENU,
        CHANGE_PROFILE,
        SHOW_EVENTS,
        SHOW_PARTNERS,
        SHOW_USERS,
        SHOW_ADMINS,
        CREATE_EVENT,
        SEND_MESSAGE_TO_EVERYBODY
    }
}
