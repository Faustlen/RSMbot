package net.dunice.mk.rsmtelegrambot.handler.state.stateobject;

import lombok.Getter;
import lombok.Setter;
import net.dunice.mk.rsmtelegrambot.entity.Category;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Setter
@Getter
public class PartnerRegistrationState {
    private PartnerRegistrationStep step = PartnerRegistrationStep.REQUEST_PARTNER_NAME;
    private String name;
    private String phoneNumber;
    private Short discountPercent;
    private Category category;
    private byte[] logo;
    private LocalDateTime discountDate;
    private String info;

    public enum PartnerRegistrationStep {
        REQUEST_PARTNER_NAME,
        VALIDATE_PARTNER_NAME,
        PHONE_NUMBER,
        DISCOUNT_PERCENT,
        CATEGORY,
        LOGO,
        DISCOUNT_DATE,
        PARTNER_INFO,
        FINISH
    }
}
