package net.dunice.mk.rsmtelegrambot.service;

import lombok.Getter;
import lombok.Setter;
import net.dunice.mk.rsmtelegrambot.constant.UserUpdateStep;

@Setter
@Getter
public class UserUpdateState {
    private UserUpdateStep step = UserUpdateStep.CONFIRM;
    private String fullName;
    private String name;
    private String phoneNumber;
    private String info;

}
