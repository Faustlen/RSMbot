package net.dunice.mk.rsmtelegrambot.handler.state.stateobject;

import lombok.Getter;
import lombok.Setter;
import net.dunice.mk.rsmtelegrambot.entity.User;

@Getter
@Setter
public class BanUserState {
    private User targetUser;
    private BanUserStep step = BanUserStep.REQUEST_USER_ID;

    public enum BanUserStep {
        REQUEST_USER_ID,
        VERIFY_USER_TO_BAN,
        CONFIRM_USER_TO_BAN,
    }
}
