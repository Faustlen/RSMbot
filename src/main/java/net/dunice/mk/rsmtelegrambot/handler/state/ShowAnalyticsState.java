package net.dunice.mk.rsmtelegrambot.handler.state;

import lombok.Getter;
import lombok.Setter;
import net.dunice.mk.rsmtelegrambot.entity.Partner;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class ShowAnalyticsState {

    private ShowAnalyticsStep step = ShowAnalyticsStep.SHOW_PARTNERS_LIST;
    private List<Partner> partners;
    private Long selectedPartnerId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    public enum ShowAnalyticsStep {
        SHOW_PARTNERS_LIST,
        SELECT_PARTNER,
        REQUEST_START_DATE,
        REQUEST_END_DATE,
    }
}
