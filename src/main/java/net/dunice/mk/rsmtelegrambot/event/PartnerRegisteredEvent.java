package net.dunice.mk.rsmtelegrambot.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.dunice.mk.rsmtelegrambot.entity.Partner;

@Getter
@Setter
@RequiredArgsConstructor
public class PartnerRegisteredEvent {

    private final Partner partner;
}
