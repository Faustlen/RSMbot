package net.dunice.mk.rsmtelegrambot.handler.state.step;

public enum UserRegistrationStep {
    REQUEST_FULL_NAME,
    VALIDATE_FULL_NAME,
    PHONE_NUMBER,
    INFO,
    RETRY_REGISTRATION,
    FINISH,
}