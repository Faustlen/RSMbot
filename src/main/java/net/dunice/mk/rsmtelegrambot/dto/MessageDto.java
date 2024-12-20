package net.dunice.mk.rsmtelegrambot.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MessageDto {
    private final String text;
    private final byte[] image;

    public MessageDto(String text, byte[] image) {
        this.text = text;
        this.image = image;
    }

    public MessageDto(String text) {
        this.text = text;
        this.image = null;
    }
}