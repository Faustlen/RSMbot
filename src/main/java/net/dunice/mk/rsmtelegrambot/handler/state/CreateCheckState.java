package net.dunice.mk.rsmtelegrambot.handler.state;

import lombok.Getter;
import lombok.Setter;
import net.dunice.mk.rsmtelegrambot.entity.Partner;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class CreateCheckState {
    private CreateCheckStep step = CreateCheckStep.DISCOUNT_CALC;
    private Partner partner;
    private BigDecimal CheckSum;
    private Short discountPercent;
    private LocalDateTime ConfirmationDate;

    public enum CreateCheckStep {
        DISCOUNT_CALC,
        ENTER_SUM,
        CONFIRM_MEMBERSHIP,
        CONFIRM_CHECK_CREATION
    }
}
