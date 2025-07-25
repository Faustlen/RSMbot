package net.dunice.mk.rsmtelegrambot.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.dunice.mk.rsmtelegrambot.entity.Partner;
import java.util.List;

@Getter
@Setter
@RequiredArgsConstructor
public class BroadcastPartnersEvent {

    private final String message;
    private final List<Partner> partners;

}