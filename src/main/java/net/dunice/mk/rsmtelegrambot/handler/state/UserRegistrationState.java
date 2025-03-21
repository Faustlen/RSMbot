package net.dunice.mk.rsmtelegrambot.handler.state;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UserRegistrationState {
    private UserRegistrationStep step = UserRegistrationStep.REQUEST_MEMBERSHIP_NUMBER;
    private Integer membershipNumber;
    private String fullName;
    private String firstName;
    private String lastName;
    private String patronymic;
    private String phoneNumber;
    private String info;
    private String birthDate;

    public enum UserRegistrationStep {
        REQUEST_MEMBERSHIP_NUMBER,
        VALIDATE_MEMBERSHIP_NUMBER,
        CHECK_CONFIRMATION,
        VALIDATE_INFO,
        FINISH,
    }
}
