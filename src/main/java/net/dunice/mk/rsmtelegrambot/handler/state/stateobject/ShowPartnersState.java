package net.dunice.mk.rsmtelegrambot.handler.state.stateobject;

import lombok.Getter;
import lombok.Setter;
import net.dunice.mk.rsmtelegrambot.entity.Partner;

import java.time.LocalDateTime;

@Getter
@Setter
public class ShowPartnersState {

    private Short discountPercent;
    private LocalDateTime discountDate;
    private Partner targetPartner;

    private ShowPartnersStep step = ShowPartnersStep.SHOW_PARTNERS_LIST;

    public enum ShowPartnersStep {
        SHOW_PARTNERS_LIST,
        SHOW_PARTNER_DETAILS,
        HANDLE_USER_ACTION,
        VERIFY_NEW_DISCOUNT_PERCENT,
        VERIFY_NEW_DISCOUNT_DATE,
        CONFIRM_CHANGE,
        FINISH
    }
}
