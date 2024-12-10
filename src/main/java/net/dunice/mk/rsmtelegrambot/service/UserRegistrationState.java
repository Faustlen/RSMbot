package net.dunice.mk.rsmtelegrambot.service;

import net.dunice.mk.rsmtelegrambot.constants.UserRegistrationStep;

public class UserRegistrationState {
    private UserRegistrationStep step = UserRegistrationStep.CONFIRM;
    private String fullName;
    private String name;
    private String phoneNumber;
    private String info;

    public UserRegistrationStep getStep() {
        return step;
    }

    public void setStep(UserRegistrationStep step) {
        this.step = step;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
