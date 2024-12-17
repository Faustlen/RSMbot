package net.dunice.mk.rsmtelegrambot.handler.state.stateobject;

import lombok.Getter;
import lombok.Setter;
import net.dunice.mk.rsmtelegrambot.entity.Category;
import net.dunice.mk.rsmtelegrambot.handler.state.step.PartnerRegistrationStep;

import java.time.LocalDate;

@Setter
@Getter
public class PartnerRegistrationState {
    private PartnerRegistrationStep step = PartnerRegistrationStep.REQUEST_PARTNER_NAME;
    private String name;
    private String phoneNumber;
    private Short discountPercent;
    private Category category;
    private byte[] logo;
    private LocalDate discountDate;
    private String info;
}
