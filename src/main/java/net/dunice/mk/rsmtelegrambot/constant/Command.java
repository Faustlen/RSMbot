package net.dunice.mk.rsmtelegrambot.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@RequiredArgsConstructor
@Getter
public enum Command {
    START("/start"),
    SUSTART("/sustart");

    private final String stringValue;

    public static boolean isValidCommand(String stringValue) {
        return Arrays.stream(values()).anyMatch(c -> c.getStringValue().equals(stringValue));
    }

    public static Command getCommandByString(String stringValue) {
        return Arrays.stream(values()).dropWhile(c -> !c.getStringValue().equals(stringValue)).findFirst()
            .orElse(null);
    }
}
