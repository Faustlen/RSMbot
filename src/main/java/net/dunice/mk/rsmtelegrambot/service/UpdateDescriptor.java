package net.dunice.mk.rsmtelegrambot.service;

import lombok.Getter;
import lombok.Setter;
import net.dunice.mk.rsmtelegrambot.handler.state.BasicState;

@Getter
@Setter
public class UpdateDescriptor {

    private UpdateType updateType;

    private Long telegramId;

    private Integer messageId;

    private String text;

    private String imageId;

    private BasicState state;
}
