package net.dunice.mk.rsmtelegrambot.handler.state;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CreateStockState {
    private StockCreationStep step = StockCreationStep.REQUEST_STOCK_IMAGE;

    private byte[] image;
    private String stockHead;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private String description;

    public enum StockCreationStep {
        REQUEST_STOCK_IMAGE,
        VALIDATE_STOCK_IMAGE,
        VALIDATE_STOCK_HEAD,
        VALIDATE_PERIOD_START,
        VALIDATE_PERIOD_END,
        VALIDATE_STOCK_DESCRIPTION,
        CONFIRM_STOCK,
        FINISH
    }
}
