package net.dunice.mk.rsmtelegrambot.service;

import net.dunice.mk.rsmtelegrambot.constant.GrantAdminStep;
import net.dunice.mk.rsmtelegrambot.entity.User;

import static net.dunice.mk.rsmtelegrambot.constant.GrantAdminStep.USER_ID;

public class GrantAdminState {
    private User targetUser;
    private GrantAdminStep step = USER_ID;

    public User getTargetUser() {
        return targetUser;
    }

    public void setTargetUser(User targetUser) {
        this.targetUser = targetUser;
    }

    public GrantAdminStep getStep() {
        return step;
    }

    public void setStep(GrantAdminStep step) {
        this.step = step;
    }
}
