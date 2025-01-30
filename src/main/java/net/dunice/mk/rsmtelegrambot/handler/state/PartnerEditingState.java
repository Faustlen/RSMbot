package net.dunice.mk.rsmtelegrambot.handler.state;

import lombok.Getter;
import lombok.Setter;
import net.dunice.mk.rsmtelegrambot.entity.Partner;

@Getter
@Setter
public class PartnerEditingState {

    PartnerEditingStep step = PartnerEditingStep.SHOW_PARTNERS_DETAILS;
    Partner partner;

    public enum PartnerEditingStep {
        SHOW_PARTNERS_DETAILS,
        CHOICE_OPTION,
        UPDATE_LOGO,
        UPDATE_NAME,
        UPDATE_CATEGORY,
        UPDATE_INFO,
        UPDATE_PHONE_NUMBER
    }
}
