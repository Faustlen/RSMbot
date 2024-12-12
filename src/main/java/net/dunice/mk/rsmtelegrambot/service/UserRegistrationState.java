package net.dunice.mk.rsmtelegrambot.service;

import lombok.Getter;
import lombok.Setter;
import net.dunice.mk.rsmtelegrambot.constant.UserRegistrationStep;

@Setter
@Getter
public class UserRegistrationState {
    private UserRegistrationStep step = UserRegistrationStep.CONFIRM;
    private String fullName;
    private String name;
    private String phoneNumber;
    private String info;

}
