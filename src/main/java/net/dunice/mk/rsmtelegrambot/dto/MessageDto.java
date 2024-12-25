package net.dunice.mk.rsmtelegrambot.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MessageDto {
    private String text;
    private byte[] image;
}